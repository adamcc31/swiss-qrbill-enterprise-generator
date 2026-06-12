# Product Requirements Document (PRD)
## Swiss QR-Bill Enterprise System

---

## 1. Document Control
- **Version**: 1.0.0
- **Status**: Draft
- **Target Audience**: Swiss Wealth Management Institutions, IT Compliance Officers, Fintech Architects

---

## 2. Product Overview
The **Swiss QR-Bill Enterprise System** is a high-performance, secure, and compliant full-stack solution for generating, validating, and displaying Swiss QR-bills. Refactored from a client-side demo, this application introduces a clean separation between a Java 21/Spring Boot REST API backend and an Angular 18 Single Page Application (SPA) frontend.

It targets Swiss private banking and wealth management workflows, providing financial agents with an intuitive dashboard to create standard-compliant bills, print them according to SIX Group guidelines, and ensure total compliance with Swiss Interbank Clearing (SIC) standards.

---

## 3. Compliance & Regulatory Requirements (SIX & FINMA)

### 3.1 SIX Group Swiss Payment Standards
The generated QR-bill must comply with the SIX Group Implementation Guidelines for the QR-bill (v2.2 / v2.3). The system enforces:
- **Physical Dimensions**: The payment slip must be exactly **210mm x 105mm**.
- **Scissor Line / Scissors Symbol**: A dotted cutting line and scissors icon must separate the payment part from the receipt.
- **QR Code Dimensions**: The QR code must be **46mm x 46mm** with a centered white-bordered Swiss Cross (**7mm x 7mm** inner black box, **3mm x 1mm** white cross arms).
- **Structure Type**: Structured addresses (Type S) are enforced for both Creditor and Debtor.
- **Allowed Currencies**: Enforce Swiss Franc (**CHF**) or Euro (**EUR**).

### 3.2 FINMA Security and Compliance Standards
As an enterprise-grade financial app, the solution aligns with FINMA circulars for operational risk, security, and data governance:
- **Zero Sensitive Data Leakage in Logs**: IBANs, names, and references are masked in application logs. Specifically, IBANs are masked keeping only the first 8 characters visible (e.g. `CH56 0483 **** **** **** *`).
- **Structured Audit Trails**: Structured logging using Slf4j and MDC (Mapped Diagnostic Context) is implemented to tag all log statements with a unique `Correlation-ID` or `Request-ID`.
- **Backend-Driven Validation**: All validation logic runs server-side to prevent bypasses, and validation errors are returned in a standard API response format.
- **Input Sanitization**: Enforce strict alphanumeric and character length constraints on all input fields.

---

## 4. Key Functional Features
1. **Interactive Bill Generation Form**: A modern Angular 18 Reactive Form that enforces real-time client-side validation hints.
2. **Dynamic Preview**: Real-time rendering of the payment slip receipt and payment part inside the web UI.
3. **SVG Vector QR Output**: Server-side generated vector SVG for the QR code, ensuring high print resolution (no pixelation) and strict SIX compliance.
4. **Validation Endpoint**: A dedicated API endpoint (`POST /api/v1/qrbill/validate`) to allow bulk validation of billing requests before generation.
5. **Print-Ready Styles**: A dedicated print media style sheet that hides all web UI elements and places the payment slip precisely at the bottom of an A4 page when printed.

---

## 5. Non-Functional Requirements
- **Performance**: QR-bill validation and generation API response times must be `< 100ms`.
- **Security**: Strict CORS policies matching the origin of the SPA, no hardcoded API secrets, and output sanitation (using `DomSanitizer` to mitigate XSS risks).
- **Technology Alignment**: Java 21, Spring Boot 3.x, Angular 18, TypeScript 5.x.
