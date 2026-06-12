import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { QrBillResponse } from '../../../../core/models/qrbill.model';

@Component({
  selector: 'app-payment-slip',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment-slip.component.html',
  styleUrls: ['./payment-slip.component.scss']
})
export class PaymentSlipComponent implements OnChanges {
  @Input() billData: QrBillResponse | null = null;
  safeQrSvg: SafeHtml | null = null;

  constructor(private sanitizer: DomSanitizer) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['billData'] && this.billData) {
      if (this.billData.qrImage) {
        // SafeHtml bypass to prevent Angular from stripping SVG tags
        this.safeQrSvg = this.sanitizer.bypassSecurityTrustHtml(this.billData.qrImage);
      } else {
        this.safeQrSvg = null;
      }
    }
  }
}
