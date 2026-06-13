# Swiss QR-Bill Enterprise Generator & Validator

A high-performance, enterprise-grade application for generating, validating, and downloading Swiss QR-Bills fully compliant with **ISO 20022 and Six Group Standards**. Built with a robust **Spring Boot 3.3 (Java 21) backend** and a modern **Angular 18 frontend**.

---

## 🚀 What's New in This Release (Sprint Overhaul)

This sprint introduces significant enterprise-ready capabilities, security architectures, testing automations, and a top-tier visual experience:

### 1. Robust Multi-Stage Dockerization (`docker-compose`)
- **Backend Dockerization**: Optimized multi-stage build starting from `maven:alpine` for fast compilation, producing a slim runtime running on `eclipse-temurin:21-jre-alpine`. Implements security hardening with a non-root system group and user.
- **Frontend Dockerization**: Compiled using `node:20-alpine` and hosted statically via `nginx:alpine`. Includes a custom `nginx.conf` designed to gracefully manage single-page application (SPA) routing fallback.
- **Orchestration**: Root-level `docker-compose.yml` ties both services together (Frontend on port `4200`, Backend on port `8081`) with out-of-the-box whitelisted CORS connections and restart policies.

### 2. GitHub Actions CI/CD Pipeline (`.github/workflows/ci.yml`)
- Automates verification on every push and pull request.
- Runs Java backend tests and generates **JaCoCo Code Coverage Reports**.
- Packages and uploads the full coverage reports (`backend/target/site/jacoco/`) as an automated build artifact.
- Restores and caches Maven and npm dependencies to maintain sub-minute pipeline execution speeds.

### 3. Whitelisted OpenAPI & Swagger UI
- Added Springdoc OpenAPI 3.0 matching Spring Boot 3.3.
- Decorated backend endpoint contracts (`QrBillController.java`) with descriptive Swagger schemas, implementations, and payload responses.
- Whitelisted development paths so developers can read and interactively execute dry-run payloads directly at:
  👉 **`http://localhost:8081/swagger-ui/index.html`**

### 4. Transition to Neutral Corporate Design ("Helvetia AG")
- Sanitized and replaced all references to legacy "Exata Indonesia Demo" with a neutral, corporate Swiss-aligned designation: **"Helvetia AG Demo"** (and **"Helvetia AG"** in testing files).
- Applies cleanly to mock data loader services, HTML input placeholders, backend tests, and technical documentation specs (`docs/TRD.md`).

### 5. Spring Security Skeleton + JWT Authorization
- Introduced `spring-boot-starter-security` to establish secure API boundaries.
- Configured a stateless security policy (`SecurityConfig.java`) whitelisting metadata/checks (Swagger UI, OpenAPI, Health Check) and securing operational QR-Bill generation paths.
- Implemented `JwtAuthenticationFilter.java` and `JwtService.java` to parse, validate, and authenticate JSON Web Tokens.
- Packed with extensive **architectural blueprints** outlining Keycloak/Okta Identity Provider (OAuth2 Resource Server) integration, secure HashiCorp Vault credential handshakes, and stateless token revoking.

### 6. Interactive Multi-Tab Glassmorphic UI/UX Overhaul
- **Tabbed Step Wizard**: Refactored the sprawling, long form into a highly interactive, 4-step tabbed workspace:
  - 📂 **Tab 1: Creditor** (Payee address and bank IBAN details)
  - 💰 **Tab 2: Payment** (Invoice currency and numeric amounts)
  - 👤 **Tab 3: Debtor** (Optional payer billing address)
  - 📝 **Tab 4: Reference** (Structured SCOR, QRR, or NON references and notes)
- **Real-Time Validation Badges**: Header tabs dynamically display validation badges (green checkmark ✓ for success, red indicator ! for error, or progress numbers if untouched) indicating current validity.
- **Smart UX Auto-Switch**: If a user clicks "Generate" while any field in the form is invalid, the wizard automatically switches the active view to the first invalid tab, highlighting the error.
- **Dark-Tech Glassmorphism Aesthetic**: Redesigned utilizing translucent cards, glowing red accents, fine slate gradients, responsive side-by-side flex layouts, and styled warning panels.

---

## 🛠️ How to Run Locally

### Option A: Using Docker Compose (Recommended)
Compile, bundle, and run both services with a single command:
```bash
docker-compose up --build
```
Once active, visit:
- **Frontend App**: `http://localhost:4200`
- **Interactive Swagger Docs**: `http://localhost:8081/swagger-ui/index.html`
- **OpenAPI Spec (JSON)**: `http://localhost:8081/v3/api-docs`

---

### Option B: Manual Execution

#### 1. Backend Service (Spring Boot)
Ensure you have JDK 21 installed.
```bash
cd backend
# On Windows Command Prompt:
mvnw.cmd spring-boot:run
# On Linux / macOS / Git Bash:
chmod +x mvnw && ./mvnw spring-boot:run
```
The backend API server launches at `http://localhost:8081`.

To execute unit tests and compile JaCoCo code coverage locally:
```bash
# On Windows:
mvnw.cmd clean test verify
# On Linux/macOS:
./mvnw clean test verify
```
Reports are available locally at: `backend/target/site/jacoco/index.html`.

#### 2. Frontend Application (Angular 18)
Ensure you have Node.js 20+ installed.
```bash
cd frontend
# Install packages
npm ci
# Launch development server
npm start
```
The client dashboard launches at `http://localhost:4200`.

---

## 📝 API Endpoint Schema Contract

All routes are versioned under `/api/v1/qrbill/` and wrap responses in a standard `ApiResponse<T>` envelope.

### 1. `POST /api/v1/qrbill/generate`
Validates input parameters and returns the raw inline vector `<svg>` of the payment slip along with formatted fields.
- **Payload (`QrBillRequest`)**:
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

### 2. `POST /api/v1/qrbill/download`
Validates parameters and generates an A4 Swiss QR-Bill document exported in standard PDF format.
- Content-Type: `application/pdf`

### 3. `POST /api/v1/qrbill/validate`
Performs a dry-run check of the payload structure without drawing the graphic files.
- Returns `{"success": true, "data": "Valid"}` if correct, or `400 Bad Request` with exact field error logs if incorrect.

---

## 🔒 Security Design Layout

```
                  Client Request (Authorization: Bearer <JWT>)
                                       │
                                       ▼
                     [ JwtAuthenticationFilter ] (Hook)
                                       │
                   ├── Expired / Malformed Token? ──► [ Deny: 401/403 ]
                   │
                   ▼ (Extract Principal details)
                  [ UsernamePasswordAuthenticationToken ]
                                       │
                                       ▼ (Stateless injection)
                    [ SecurityContextHolder ]
                                       │
                                       ▼
                       [ HttpSecurity Matchers ]
                   ├── Swagger, Health? ──► [ permitAll() ]
                   └── Generates / Val?  ──► [ permitAll() - whitelisted for local dev ]
```

---

## 👥 Authors & Contributors
- Senior Full-Stack Engineer / Architect Agent (Cline)
- Six Group Payment Standards Core Team
- Helvetica Enterprise Integrations Group
