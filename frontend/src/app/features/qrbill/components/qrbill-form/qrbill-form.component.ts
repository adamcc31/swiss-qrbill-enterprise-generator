import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { QrBillRequest } from '../../../../core/models/qrbill.model';
import { QrBillService } from '../../../../core/services/qrbill.service';

@Component({
  selector: 'app-qrbill-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './qrbill-form.component.html',
  styleUrls: ['./qrbill-form.component.scss']
})
export class QrBillFormComponent implements OnInit {
  @Input() isGenerated: boolean = false;
  @Output() formSubmit = new EventEmitter<QrBillRequest>();
  @Output() printTrigger = new EventEmitter<void>();

  qrForm!: FormGroup;
  referenceHint: string = 'Select a reference type';
  activeTab: number = 0; // 0: Creditor, 1: Payment, 2: Debtor, 3: Ref & Msg

  constructor(private fb: FormBuilder, private qrBillService: QrBillService) {}

  setTab(index: number): void {
    this.activeTab = index;
  }

  // Getters for individual tab validity status
  get isCreditorTabValid(): boolean {
    const creditorAddress = this.qrForm.get('creditorAddress');
    const creditorIban = this.qrForm.get('creditorIban');
    return !!(creditorAddress && creditorAddress.valid && creditorIban && creditorIban.valid);
  }

  get isPaymentTabValid(): boolean {
    const amount = this.qrForm.get('amount');
    const currency = this.qrForm.get('currency');
    return !!(amount && amount.valid && currency && currency.valid);
  }

  get isDebtorTabValid(): boolean {
    const debtorAddress = this.qrForm.get('debtorAddress');
    return !!(debtorAddress && debtorAddress.valid);
  }

  get isReferenceTabValid(): boolean {
    const reference = this.qrForm.get('reference');
    const message = this.qrForm.get('message');
    return !!(reference && reference.valid && message && message.valid);
  }

  // Tells if a tab is touched/dirty AND has validation errors
  get tabHasErrors(): boolean[] {
    const creditorInvalid = !this.isCreditorTabValid && 
      (this.qrForm.get('creditorAddress')?.touched || this.qrForm.get('creditorIban')?.touched);
    
    const paymentInvalid = !this.isPaymentTabValid && 
      (this.qrForm.get('amount')?.touched || this.qrForm.get('currency')?.touched);
    
    const debtorInvalid = !this.isDebtorTabValid && 
      this.qrForm.get('debtorAddress')?.touched;
    
    const refInvalid = !this.isReferenceTabValid && 
      (this.qrForm.get('reference')?.touched || this.qrForm.get('message')?.touched);

    return [!!creditorInvalid, !!paymentInvalid, !!debtorInvalid, !!refInvalid];
  }

  ngOnInit(): void {
    this.initForm();
    this.setupFormSubscriptions();
    this.loadMockData();
  }

  private initForm(): void {
    this.qrForm = this.fb.group({
      creditorAddress: this.fb.group({
        name: ['', [Validators.required, Validators.maxLength(70)]],
        street: ['', [Validators.required, Validators.maxLength(70)]],
        houseNo: ['', [Validators.required, Validators.maxLength(16)]],
        postalCode: ['', [Validators.required, Validators.maxLength(16)]],
        town: ['', [Validators.required, Validators.maxLength(35)]],
        countryCode: ['CH', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]]
      }),
      creditorIban: ['', [Validators.required]],
      amount: [null, [Validators.min(0.01)]],
      currency: ['CHF', [Validators.required]],
      debtorAddress: this.fb.group({
        name: ['', [Validators.maxLength(70)]],
        street: ['', [Validators.maxLength(70)]],
        houseNo: ['', [Validators.maxLength(16)]],
        postalCode: ['', [Validators.maxLength(16)]],
        town: ['', [Validators.maxLength(35)]],
        countryCode: ['CH', [Validators.minLength(2), Validators.maxLength(2)]]
      }),
      reference: this.fb.group({
        referenceType: ['NON', [Validators.required]],
        referenceNumber: ['']
      }),
      message: ['', [Validators.maxLength(140)]]
    });
  }

  private setupFormSubscriptions(): void {
    // Watch for Reference Type changes
    this.qrForm.get('reference.referenceType')?.valueChanges.subscribe(type => {
      this.updateReferenceValidators(type);
    });

    // QRR Check Digit Auto-calculator
    this.qrForm.get('reference.referenceNumber')?.valueChanges.subscribe(val => {
      const type = this.qrForm.get('reference.referenceType')?.value;
      if (type === 'QRR' && val) {
        const clean = val.replace(/\s+/g, '');
        if (clean.length === 26 && /^\d+$/.test(clean)) {
          const checkDigit = this.calculateModulo10Recursive(clean);
          this.qrForm.get('reference.referenceNumber')?.setValue(clean + checkDigit, { emitEvent: false });
        }
      }
    });

    // Enforce debtor address rules if name is supplied
    this.qrForm.get('debtorAddress.name')?.valueChanges.subscribe(name => {
      const debtorGroup = this.qrForm.get('debtorAddress') as FormGroup;
      const fields = ['street', 'houseNo', 'postalCode', 'town', 'countryCode'];
      
      if (name && name.trim().length > 0) {
        fields.forEach(field => {
          debtorGroup.get(field)?.setValidators([Validators.required]);
          debtorGroup.get(field)?.updateValueAndValidity({ emitEvent: false });
        });
      } else {
        fields.forEach(field => {
          debtorGroup.get(field)?.clearValidators();
          debtorGroup.get(field)?.updateValueAndValidity({ emitEvent: false });
        });
      }
    });
  }

  private updateReferenceValidators(type: string): void {
    const refNumControl = this.qrForm.get('reference.referenceNumber');
    if (!refNumControl) return;

    if (type === 'NON') {
      refNumControl.clearValidators();
      refNumControl.setValue('');
      this.referenceHint = 'No reference number allowed for NON type.';
    } else {
      refNumControl.setValidators([Validators.required]);
      if (type === 'QRR') {
        this.referenceHint = 'For QRR: 27 digits (numeric, Mod 10 recursive). Type 26 digits to auto-calculate the 27th check digit!';
      } else if (type === 'SCOR') {
        this.referenceHint = 'For SCOR: Starts with RF + 2 check digits + up to 21 alphanumeric characters (ISO 11649).';
      }
    }
    refNumControl.updateValueAndValidity();
  }

  private calculateModulo10Recursive(reference: string): number {
    const table = [0, 9, 4, 6, 8, 2, 7, 1, 3, 5];
    let carry = 0;
    for (let i = 0; i < reference.length; i++) {
      const digit = parseInt(reference.charAt(i), 10);
      carry = table[(carry + digit) % 10];
    }
    return (10 - carry) % 10;
  }

  loadMockData(): void {
    this.qrForm.patchValue({
      creditorAddress: {
        name: 'Helvetia AG Demo',
        street: 'Bahnhofstrasse',
        houseNo: '1',
        postalCode: '8001',
        town: 'Zürich',
        countryCode: 'CH'
      },
      creditorIban: 'CH56 0483 5012 3456 7800 9',
      amount: 1250.00,
      currency: 'CHF',
      debtorAddress: {
        name: 'Test Client',
        street: 'Teststrasse',
        houseNo: '5',
        postalCode: '3000',
        town: 'Bern',
        countryCode: 'CH'
      },
      reference: {
        referenceType: 'SCOR',
        referenceNumber: 'RF18 5390 0754 7034'
      },
      message: 'Invoice 1042'
    });
    this.updateReferenceValidators('SCOR');
  }

  private getRequestPayload(): QrBillRequest {
    const formValue = this.qrForm.value;
    return {
      creditorAddress: {
        name: formValue.creditorAddress.name.trim(),
        street: formValue.creditorAddress.street.trim(),
        houseNo: formValue.creditorAddress.houseNo.trim(),
        postalCode: formValue.creditorAddress.postalCode.trim(),
        town: formValue.creditorAddress.town.trim(),
        countryCode: formValue.creditorAddress.countryCode.trim().toUpperCase()
      },
      creditorIban: formValue.creditorIban.replace(/\s+/g, '').toUpperCase(),
      amount: formValue.amount ? parseFloat(formValue.amount) : null,
      currency: formValue.currency,
      debtorAddress: formValue.debtorAddress.name ? {
        name: formValue.debtorAddress.name.trim(),
        street: formValue.debtorAddress.street.trim(),
        houseNo: formValue.debtorAddress.houseNo.trim(),
        postalCode: formValue.debtorAddress.postalCode.trim(),
        town: formValue.debtorAddress.town.trim(),
        countryCode: formValue.debtorAddress.countryCode.trim().toUpperCase()
      } : null,
      reference: {
        referenceType: formValue.reference.referenceType,
        referenceNumber: formValue.reference.referenceNumber.replace(/\s+/g, '').toUpperCase()
      },
      message: formValue.message ? formValue.message.trim() : ''
    };
  }

  onSubmit(): void {
    if (this.qrForm.valid) {
      this.formSubmit.emit(this.getRequestPayload());
    } else {
      // Mark all as touched to display validation errors
      this.qrForm.markAllAsTouched();
      
      // Auto-switch to the first tab that has validation errors for superior UX
      if (!this.isCreditorTabValid) {
        this.activeTab = 0;
      } else if (!this.isPaymentTabValid) {
        this.activeTab = 1;
      } else if (!this.isDebtorTabValid) {
        this.activeTab = 2;
      } else if (!this.isReferenceTabValid) {
        this.activeTab = 3;
      }
    }
  }

  onDownloadPdf(): void {
    if (this.qrForm.valid) {
      const request = this.getRequestPayload();
      this.qrBillService.downloadPdf(request).subscribe({
        next: (response) => {
          const contentDisposition = response.headers.get('content-disposition');
          let filename = 'swiss-qrbill.pdf';
          if (contentDisposition) {
            const matches = /filename="([^"]+)"/.exec(contentDisposition);
            if (matches && matches[1]) {
              filename = matches[1];
            }
          }
          const blob = response.body;
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = filename;
          link.click();
          window.URL.revokeObjectURL(url);
        },
        error: (err) => {
          console.error('Failed to download PDF:', err);
        }
      });
    } else {
      this.qrForm.markAllAsTouched();
    }
  }

  onPrint(): void {
    this.printTrigger.emit();
  }

  // Helper validation getters
  isInvalid(controlPath: string): boolean {
    const control = this.qrForm.get(controlPath);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  getErrorMessage(controlPath: string, fieldName: string): string {
    const control = this.qrForm.get(controlPath);
    if (!control || !control.errors) return '';
    if (control.errors['required']) return `${fieldName} is required.`;
    if (control.errors['maxlength']) return `${fieldName} cannot exceed ${control.errors['maxlength'].requiredLength} characters.`;
    if (control.errors['minlength']) return `${fieldName} must be at least ${control.errors['minlength'].requiredLength} characters.`;
    if (control.errors['min']) return `${fieldName} must be greater than 0.`;
    return '';
  }
}
