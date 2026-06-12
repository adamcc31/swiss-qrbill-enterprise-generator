# Swiss QR-Bill Enterprise System
### Production-Grade Swiss Fintech & Payment Processing Architecture

This repository contains a full-stack, enterprise-grade refactoring of the Swiss QR-bill generation and validation system, specifically designed to target the strict standards of **Swiss Wealth Management and Private Banking Institutions**. 

The architecture features a clean separation between a high-performance **Java 21 / Spring Boot 3.x REST API** backend and a responsive **Angular 18 SPA** frontend utilizing strict type checking and custom SCSS engineered to replicate physical payment slips matching the official **SIX Group guidelines**.

---

## Key Highlights & Enterprise Features
* **Compliance with SIX Group Standards**: Renders physical-sized payment slips (**210mm x 105mm**) containing a dotted cutting line, scissors icon, and a server-side generated **46mm x 46mm vector SVG QR code** with a baked-in Swiss Cross overlay.
* **Granular Server-Side Validation**: Ported from client-side JS into a robust Java validation layer. Verifies **ISO 7064 Modulo 97-10** IBAN checksums, **QR-IBAN IID** range rules, **Modulo 10 recursive** QRR checksums, and **ISO 11649 Modulo 97** SCOR checksums.
* **FINMA-Aligned Operational Practices**:
  * **Input Sanitization**: Rejects/escapes all parameters to prevent injection.
  * **MDC Structured Logging**: Correlation IDs trace requests from the HTTP filter down to the controller and services.
  * **Sensitive Data Masking**: Automatically masks IBANs after the first 8 characters in log outputs to prevent PII leakage.
* **Print-Ready Stylesheet**: Uses CSS `@media print` rules to automatically hide the dashboard UI and position the payment slip precisely at the bottom of an A4 page when printed.

---

## Technology Stack

### Backend
* **Runtime**: Java 21 / Oracle JDK
* **Framework**: Spring Boot 3.3.0
* **Build System**: Maven 3.9.6 (Included as portable wrapper)
* **Swiss QR Library**: `net.codecrete.qrbill:qrbill-generator:3.4.0`
* **Logging**: Slf4j + MDC + Logback

### Frontend
* **Runtime**: Node.js (v18+)
* **Framework**: Angular 18.x
* **Language**: TypeScript 5.4.x (Strict compilation enabled)
* **Styling**: Vanilla SCSS + Angular Material 18.x (pinned in `package.json`)

---

## Project Structure

```
swiss-qrbill-enterprise/
├── docs/                     # Compliance & Specifications
│   ├── PRD.md                # Product Requirements Document
│   ├── ERD.md                # Entity-Relationship Diagram (Mermaid)
│   └── TRD.md                # Technical Requirements Document
├── backend/                  # Java / Spring Boot API
│   ├── maven/                # Self-contained portable Maven installation
│   ├── src/main/java/com/exata/swissqrbill/
│   │   ├── SwissQrBillApplication.java
│   │   ├── config/           # CORS & Logging Correlation Filters
│   │   ├── controller/       # Rest API Controller
│   │   ├── exception/        # Exception handlers
│   │   ├── model/            # Request/Response DTOs & Error schemas
│   │   └── service/          # Validation & QR Generation logic
│   ├── pom.xml
│   └── mvnw.cmd              # Windows Maven wrapper script
└── frontend/                 # Angular 18 SPA
    ├── src/app/
    │   ├── core/
    │   │   ├── models/       # TypeScript models
    │   │   ├── services/     # API Client Service
    │   │   └── interceptors/ # HTTP Interceptors
    │   ├── features/
    │   │   └── qrbill/       # Orchestrator & child components
    │   └── shared/           # Reusable API error components
    ├── angular.json
    ├── package.json
    └── tsconfig.json
```

---

## Automated Tests & Quality Gate
[![Coverage](https://img.shields.io/badge/Coverage-93%25-brightgreen.svg)](#)

The project enforces a strict quality gate in the CI/CD pipeline using **JaCoCo**.
* **Coverage Mandate**: Builds will fail automatically if line coverage drops below **85%** in `com.exata.swissqrbill.service.*` (generation/validation) or `com.exata.swissqrbill.exception.*` (error payloads/handlers).
* **Test Verification**: Runs 47 automated tests verifying Modulo 97, Modulo 10, ISO 11649, CORS headers, error mappings, and try-with-resources resource handling.

To execute the test suite and check coverage:
1. Navigate to the `backend/` directory.
2. Run the Maven verify lifecycle:
   ```cmd
   .\mvnw.cmd verify
   ```
3. Open the generated HTML report at:
   `backend/target/site/jacoco/index.html`

---

## Production Hardening & Rate Limiting

### IP Extraction Behind Proxies
The system includes `RateLimitFilter` (using Bucket4j) registered at order `1` to prevent denial-of-service spikes. It secures remote IP extraction using Tomcat's container-native `RemoteIpValve` configured via Spring Boot's application properties:
```yaml
server:
  forward-headers-strategy: framework
  tomcat:
    remoteip:
      internal-proxies: ".*"
```
This configuration ensures that clients cannot spoof their IP address by injecting raw `X-Forwarded-For` headers.

### Architectural Trade-off: In-Memory vs. Distributed State
> [!NOTE]
> The current rate-limiting buckets are stored in-memory using `ConcurrentHashMap`.
> * **Applicability**: This is a pragmatic, zero-dependency trade-off suitable for single-node deployments, local testing, and portfolio demonstrations.
> * **Horizontal Scaling**: In a multi-node, horizontally scaled Kubernetes or Cloud environment, traffic is load-balanced across multiple nodes, causing rate limits to be tracked independently per instance. For production cloud scaling, the `ConcurrentHashMap` proxy maps should be replaced with a distributed Redis-backed `ProxyManager` (e.g. `bucket4j-redis` extension) to share rate-limiting tokens across all API nodes.

---

## Getting Started

### 1. Run the Backend (API)
Ensure you have a Java 21 JDK installed on your PATH.

1. Navigate to the `backend/` directory:
   ```cmd
   cd backend
   ```
2. Start the Spring Boot application using the local Maven wrapper:
   ```cmd
   .\mvnw.cmd spring-boot:run
   ```
3. The server starts on port **`8081`** (updated from `8080` to avoid local XAMPP/Apache conflicts). You can check health at:
   `http://localhost:8081/api/v1/qrbill/health`

### 2. Run the Frontend (Angular SPA)
Ensure you have Node.js and the Angular CLI installed.

1. Navigate to the `frontend/` directory:
   ```cmd
   cd ../frontend
   ```
2. Install dependencies:
   ```cmd
   npm install
   ```
3. Start the dev server:
   ```cmd
   npm start
   ```
4. Access the application in your browser at:
   `http://localhost:4200`

---

## API Specifications

All endpoints are versioned under `/api/v1/` and wrapped in a standard `ApiResponse<T>` envelope.

### POST `/api/v1/qrbill/generate`
Validates and generates the QR-bill SVG image along with formatted slip details.

* **Request Example**:
```json
{
  "creditorAddress": {
    "name": "Exata Indonesia Demo",
    "street": "Bahnhofstrasse",
    "houseNo": "1",
    "postalCode": "8001",
    "town": "Zürich",
    "countryCode": "CH"
  },
  "creditorIban": "CH5604835012345678009",
  "amount": 1250.00,
  "currency": "CHF",
  "reference": {
    "referenceType": "SCOR",
    "referenceNumber": "RF18539007547034"
  },
  "message": "Invoice 1042"
}
```

* **Response Example (Success)**:
```json
{
  "success": true,
  "data": {
    "qrImage": "<svg ...>...</svg>",
    "formattedCreditorIban": "CH56 0483 5012 3456 7800 9",
    "formattedReferenceNumber": "RF18 5390 0754 7034",
    "formattedAmount": "1'250.00",
    "creditorAddressHtml": "Exata Indonesia Demo<br>Bahnhofstrasse 1<br>8001 Zürich",
    "debtorAddressHtml": "<div ...></div>",
    "currency": "CHF",
    "message": "Invoice 1042",
    "hasAmount": true
  },
  "errors": []
}
```

---

## Swiss Compliance & Validation Rules

The backend validation layer implements the following validation criteria:
1. **Creditor & Debtor Addresses**: Address elements (Name, Street, House Number, Postal Code, Town) must not be empty. Country must be a 2-letter ISO code (`CH` or `LI`).
2. **IBAN Checksum**: Validates the Modulo 97-10 IBAN checksum. 
3. **QRR vs standard IBAN**:
   * If Reference Type is `QRR` (QR-Reference), the IBAN must be a QR-IBAN (IID range `30000` to `31999`). Reference must be exactly 27 numeric digits with a valid Modulo 10 recursive check digit.
   * If Reference Type is `SCOR` (Structured Creditor Reference) or `NON` (No Reference), the IBAN must be a standard IBAN (NOT a QR-IBAN). SCOR references must be valid ISO 11649 references (prefixed with `RF` and Modulo 97 compliant).
4. **Amount Limits**: If specified, the amount must be greater than `0.00` and have a maximum of 12 digits and 2 decimal places.
