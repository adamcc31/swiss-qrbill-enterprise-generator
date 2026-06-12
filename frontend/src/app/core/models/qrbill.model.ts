export interface Address {
  name: string;
  street: string;
  houseNo: string;
  postalCode: string;
  town: string;
  countryCode: string;
}

export interface PaymentReference {
  referenceType: 'QRR' | 'SCOR' | 'NON';
  referenceNumber?: string;
}

export interface QrBillRequest {
  creditorAddress: Address;
  creditorIban: string;
  amount?: number | null;
  currency: 'CHF' | 'EUR';
  debtorAddress?: Address | null;
  reference: PaymentReference;
  message?: string;
}

export interface QrBillResponse {
  qrImage: string; // SVG content
  formattedCreditorIban: string;
  formattedReferenceNumber: string;
  formattedAmount: string;
  creditorAddressHtml: string;
  debtorAddressHtml: string;
  currency: string;
  message: string;
  hasAmount: boolean;
}

export interface ValidationError {
  field: string;
  message: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  errors: ValidationError[];
}
