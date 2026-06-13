# Technical Requirements Document (TRD)
## Swiss QR-Bill Enterprise System

---

## 1. API Contracts & Endpoint Design

All endpoints are versioned under `/api/v1/`. Responses are always wrapped in the generic `ApiResponse<T>` envelope.

### 1.1 GET `/api/v1/qrbill/health`
Returns the status of the server.
- **Response Status**: `200 OK`
- **Response Payload**:
  ```json
  {
    "success": true,
    "data": "Healthy",
    "errors": []
  }
  ```

### 1.2 POST `/api/v1/qrbill/validate`
Validates a QR-bill request without generating the QR image. Useful for dry-runs.
- **Request Payload**: (Same as `QrBillRequest` below)
- **Response Status**: `200 OK` if valid; `400 Bad Request` if invalid.
- **Response Payload (Valid)**:
  ```json
  {
    "success": true,
    "data": "Valid",
    "errors": []
  }
  ```
- **Response Payload (Invalid)**:
  ```json
  {
    "success": false,
    "data": null,
    "errors": [
      {
        "field": "creditorIban",
        "message": "Invalid Swiss/Liechtenstein IBAN checksum."
      }
    ]
  }
  ```

### 1.3 POST `/api/v1/qrbill/generate`
Validates and generates the QR code SVG and slip details.
- **Request Payload (`QrBillRequest`)**:
  ```json
  {
    "creditorAddress": {
      "name": "Helvetia AG Demo",
      "street": "Bahnhofstrasse",
      "houseNo": "1",
      "postalCode": "8001",
      "town": "Zürich",
      "countryCode": "CH"
    },
    "creditorIban": "CH5604835012345678009",
    "amount": 1250.00,
    "currency": "CHF",
    "debtorAddress": {
      "name": "Test Client",
      "street": "Teststrasse",
      "houseNo": "5",
      "postalCode": "3000",
      "town": "Bern",
      "countryCode": "CH"
    },
    "reference": {
      "referenceType": "SCOR",
      "referenceNumber": "RF18539007547034"
    },
    "message": "Invoice 1042"
  }
  ```
- **Response Status**: `200 OK` if successful; `400 Bad Request` if validation fails.
- **Response Payload (Successful)**:
  ```json
  {
    "success": true,
    "data": {
      "qrImage": "<svg ...>...</svg>",
      "formattedCreditorIban": "CH56 0483 5012 3456 7800 9",
      "formattedReferenceNumber": "RF18 5390 0754 7034",
      "formattedAmount": "1'250.00",
      "creditorAddressHtml": "Helvetia AG Demo<br>Bahnhofstrasse 1<br>8001 Zürich",
      "debtorAddressHtml": "Test Client<br>Teststrasse 5<br>CH-3000 Bern",
      "currency": "CHF",
      "message": "Invoice 1042",
      "hasAmount": true
    },
    "errors": []
  }
  ```

---

## 2. Validation Rules Matrix

These rules represent a migration of the validation logic from the original client-side `app.js` to the backend's `ValidationService`.

| Validation Target | Rule Type | Logic / Constraint | Action on Failure |
| :--- | :--- | :--- | :--- |
| **Creditor Address** | Field Validation | Must exist; Name, Street, HouseNo, Zip, City must be non-empty. Country must be 2-letter ISO code (`CH` or `LI`). | Reject with HTTP 400 |
| **Creditor IBAN** | Checksum | ISO 7064 Modulo 97-10 checksum must equal 1. Must start with `CH` or `LI` and be 21 characters long. | Reject with HTTP 400 |
| **Amount** | Optional Check | If provided, must be `> 0.00` and have a maximum of 12 digits and 2 decimal places. | Reject with HTTP 400 |
| **Debtor Address** | Conditional | If Debtor Name is specified, all debtor address fields (Street, HouseNo, Zip, City, Country) become mandatory. Country must be 2-letter ISO. | Reject with HTTP 400 |
| **QR-Reference (QRR)** | Format / Check | Must be exactly 27 numeric digits. Check digit is the 27th character, computed using Modulo 10 recursive. | Reject with HTTP 400 |
| **SCOR Reference** | Format / Check | Starts with `RF` + 2 check digits + 5-21 alphanumeric chars. ISO 11649 Modulo 97 check must equal 1. | Reject with HTTP 400 |
| **QRR vs IBAN** | Cross-Validation | If `referenceType` is `QRR`, `creditorIban` must be a QR-IBAN (IID range `30000` to `31999`). | Reject with HTTP 400 |
| **SCOR/NON vs IBAN** | Cross-Validation | If `referenceType` is `SCOR` or `NON`, `creditorIban` must NOT be a QR-IBAN (must be standard IBAN). | Reject with HTTP 400 |

---

## 3. Security Considerations

### 3.1 Angular DomSanitizer Injection
To protect the application against Cross-Site Scripting (XSS) while allowing the server-rendered vector QR code to display correctly:
- The backend returns the QR code as a raw SVG string.
- Angular’s default behavior is to sanitize all HTML elements bound to `[innerHTML]`, which stripping the `<svg>` contents or tags.
- In `payment-slip.component.ts`, we inject `DomSanitizer` and mark the SVG string as safe using `DomSanitizer.bypassSecurityTrustHtml(qrImage)`.
- *Security rationale*: Since the SVG is generated strictly server-side by our own API (which validates all input parameters and uses a trusted library), it is safe to bypass sanitization for this specific field.

### 3.2 CORS (Cross-Origin Resource Sharing)
The backend's `CorsConfig` is configured to allow requests exclusively from `http://localhost:4200` during development. Production configurations should restrict this to the exact domain hosting the SPA.

### 3.3 Input Sanitization
Both front-end Reactive Forms and backend controllers sanitize input values. Empty spaces are trimmed, and any HTML tag structures are stripped or rejected to prevent injection.

---

## 4. FINMA Alignment Patterns

### 4.1 Structured Logging (Correlation IDs)
We configure a `OncePerRequestFilter` to intercept incoming HTTP requests, extract or generate a unique `Correlation-ID` header, and load it into Logback's MDC (Mapped Diagnostic Context). The correlation ID is included in every log line output, enabling end-to-end tracing.

### 4.2 Data Masking in Logs
All logging statements containing sensitive customer details or financial identifiers are masked. Specifically, any log statement referencing an IBAN will invoke a helper utility to mask the account number after the first 8 characters:
- Raw: `CH5604835012345678009`
- Masked: `CH560483 *********`

---

## 5. Component Tree Diagram

The Angular SPA uses the following standalone component layout:

```
AppComponent
 └── QrBillComponent (Parent Coordinator)
      ├── ApiErrorComponent (Displays global/validation errors)
      ├── QrBillFormComponent (Reactive form for input & validation hints)
      └── PaymentSlipComponent (Renders A4 & 210x105mm printable payment slip)
```
