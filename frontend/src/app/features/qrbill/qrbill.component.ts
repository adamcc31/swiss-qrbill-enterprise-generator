import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { QrBillService } from '../../core/services/qrbill.service';
import { QrBillRequest, QrBillResponse, ValidationError } from '../../core/models/qrbill.model';
import { QrBillFormComponent } from './components/qrbill-form/qrbill-form.component';
import { PaymentSlipComponent } from './components/payment-slip/payment-slip.component';
import { ApiErrorComponent } from '../../shared/components/api-error/api-error.component';

@Component({
  selector: 'app-qrbill',
  standalone: true,
  imports: [CommonModule, QrBillFormComponent, PaymentSlipComponent, ApiErrorComponent],
  templateUrl: './qrbill.component.html',
  styleUrls: ['./qrbill.component.scss']
})
export class QrBillComponent {
  billData: QrBillResponse | null = null;
  apiErrors: ValidationError[] = [];

  constructor(private qrBillService: QrBillService) {}

  onFormSubmit(request: QrBillRequest): void {
    this.apiErrors = [];
    this.qrBillService.generate(request).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.billData = response.data;
          this.apiErrors = [];
        } else {
          this.billData = null;
          this.apiErrors = response.errors || [];
        }
      },
      error: (err) => {
        this.billData = null;
        if (err.error && err.error.errors) {
          this.apiErrors = err.error.errors;
        } else {
          this.apiErrors = [{
            field: 'API Connection',
            message: 'Unable to communicate with the QR-bill backend server.'
          }];
        }
      }
    });
  }

  onPrint(): void {
    window.print();
  }
}
