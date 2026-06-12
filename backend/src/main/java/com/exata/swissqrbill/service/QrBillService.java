package com.exata.swissqrbill.service;

import com.exata.swissqrbill.exception.QrBillException;
import com.exata.swissqrbill.model.Address;
import com.exata.swissqrbill.model.QrBillRequest;
import com.exata.swissqrbill.model.QrBillResponse;
import com.exata.swissqrbill.model.ValidationError;
import net.codecrete.qrbill.generator.Bill;
import net.codecrete.qrbill.generator.BillFormat;
import net.codecrete.qrbill.generator.GraphicsFormat;
import net.codecrete.qrbill.generator.Language;
import net.codecrete.qrbill.generator.OutputSize;
import net.codecrete.qrbill.generator.QRBill;
import net.codecrete.qrbill.generator.QRBillValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;

@Service
public class QrBillService {

    private static final Logger log = LoggerFactory.getLogger(QrBillService.class);
    private final ValidationService validationService;

    // Constructor injection
    public QrBillService(ValidationService validationService) {
        this.validationService = validationService;
    }

    private Bill buildBill(QrBillRequest request) {
        Bill bill = new Bill();
        
        // Set IBAN (account)
        bill.setAccount(request.getCreditorIban().replaceAll("\\s+", "").toUpperCase());
        
        // Set Amount
        if (request.getAmount() != null) {
            bill.setAmount(request.getAmount());
        }
        
        // Set Currency
        bill.setCurrency(request.getCurrency().trim().toUpperCase());

        // Set Creditor Address
        net.codecrete.qrbill.generator.Address libCreditor = new net.codecrete.qrbill.generator.Address();
        libCreditor.setName(request.getCreditorAddress().getName().trim());
        libCreditor.setStreet(request.getCreditorAddress().getStreet().trim());
        libCreditor.setHouseNo(request.getCreditorAddress().getHouseNo().trim());
        libCreditor.setPostalCode(request.getCreditorAddress().getPostalCode().trim());
        libCreditor.setTown(request.getCreditorAddress().getTown().trim());
        libCreditor.setCountryCode(request.getCreditorAddress().getCountryCode().trim().toUpperCase());
        bill.setCreditor(libCreditor);

        // Set Debtor Address (if provided)
        if (request.getDebtorAddress() != null && request.getDebtorAddress().getName() != null 
                && !request.getDebtorAddress().getName().trim().isEmpty()) {
            net.codecrete.qrbill.generator.Address libDebtor = new net.codecrete.qrbill.generator.Address();
            libDebtor.setName(request.getDebtorAddress().getName().trim());
            libDebtor.setStreet(request.getDebtorAddress().getStreet().trim());
            libDebtor.setHouseNo(request.getDebtorAddress().getHouseNo().trim());
            libDebtor.setPostalCode(request.getDebtorAddress().getPostalCode().trim());
            libDebtor.setTown(request.getDebtorAddress().getTown().trim());
            libDebtor.setCountryCode(request.getDebtorAddress().getCountryCode().trim().toUpperCase());
            bill.setDebtor(libDebtor);
        }

        // Set Reference
        String refType = request.getReference().getReferenceType().trim().toUpperCase();
        if (!"NON".equals(refType) && request.getReference().getReferenceNumber() != null) {
            bill.setReference(request.getReference().getReferenceNumber().replaceAll("\\s+", "").toUpperCase());
        }

        // Set Message (Unstructured Message)
        if (request.getMessage() != null && !request.getMessage().trim().isEmpty()) {
            bill.setUnstructuredMessage(request.getMessage().trim());
        }

        return bill;
    }

    public QrBillResponse generateQrBill(QrBillRequest request) {
        // Enforce validations
        List<ValidationError> validationErrors = validationService.validateRequest(request);
        if (!validationErrors.isEmpty()) {
            throw new QrBillException("Validation failed for QR-bill request.", validationErrors);
        }

        try {
            // Masking sensitive data in logs for compliance
            String maskedIban = maskIban(request.getCreditorIban());
            log.info("Generating Swiss QR-bill for Creditor IBAN: {}, Currency: {}, Amount: {}", 
                    maskedIban, request.getCurrency(), request.getAmount());

            // 1. Initialize Bill data
            Bill bill = buildBill(request);

            // 2. Set Graphics Format (SVG QR Code Only)
            BillFormat format = new BillFormat();
            format.setGraphicsFormat(GraphicsFormat.SVG);
            format.setOutputSize(OutputSize.QR_CODE_ONLY);
            format.setLanguage(Language.DE);
            bill.setFormat(format);

            // 3. Generate QR Code bytes
            byte[] qrBytes = QRBill.generate(bill);
            String qrImageSvg = new String(qrBytes, StandardCharsets.UTF_8);

            // 4. Map formatted fields for response
            String refType = request.getReference().getReferenceType().trim().toUpperCase();
            String formattedCreditorIban = formatIBAN(request.getCreditorIban());
            String formattedReferenceNumber = formatReference(refType, request.getReference().getReferenceNumber());
            String formattedAmount = formatAmount(request.getAmount());
            String creditorAddressHtml = formatAddressHtml(request.getCreditorAddress(), false);
            String debtorAddressHtml = formatAddressHtml(request.getDebtorAddress(), true);

            return new QrBillResponse(
                    qrImageSvg,
                    formattedCreditorIban,
                    formattedReferenceNumber,
                    formattedAmount,
                    creditorAddressHtml,
                    debtorAddressHtml,
                    request.getCurrency(),
                    request.getMessage(),
                    request.getAmount() != null
            );

        } catch (QRBillValidationError e) {
            log.error("Internal QRBill library validation failed: {}", e.getMessage(), e);
            throw new QrBillException("Invalid bill parameters according to Swiss payment standards.", e);
        } catch (Exception e) {
            log.error("Unexpected error generating QR bill: {}", e.getMessage(), e);
            throw new QrBillException("Failed to generate QR-bill due to an internal server error.", e);
        }
    }

    public byte[] generatePdf(QrBillRequest request) {
        // Enforce validations
        List<ValidationError> validationErrors = validationService.validateRequest(request);
        if (!validationErrors.isEmpty()) {
            throw new QrBillException("Validation failed for QR-bill PDF request.", validationErrors);
        }

        try {
            String maskedIban = maskIban(request.getCreditorIban());
            log.info("Generating Swiss QR-bill PDF for Creditor IBAN: {}, Currency: {}, Amount: {}", 
                    maskedIban, request.getCurrency(), request.getAmount());

            Bill bill = buildBill(request);
            
            BillFormat format = new BillFormat();
            format.setGraphicsFormat(GraphicsFormat.PDF);
            format.setOutputSize(OutputSize.A4_PORTRAIT_SHEET);
            format.setLanguage(Language.DE);
            bill.setFormat(format);

            // Use try-with-resources for ByteArrayOutputStream to prevent memory leaks
            try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                byte[] pdfBytes = QRBill.generate(bill);
                baos.write(pdfBytes);
                return baos.toByteArray();
            }

        } catch (QRBillValidationError e) {
            log.error("Internal QRBill library validation failed for PDF: {}", e.getMessage(), e);
            throw new QrBillException("Invalid bill parameters according to Swiss payment standards.", e);
        } catch (Exception e) {
            log.error("Unexpected error generating QR bill PDF: {}", e.getMessage(), e);
            throw new QrBillException("Failed to generate QR-bill PDF due to an internal server error.", e);
        }
    }

    // --- Helper Formatting Utilities ---

    private String formatIBAN(String iban) {
        if (iban == null) return "";
        String clean = iban.replaceAll("\\s+", "").toUpperCase();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clean.length(); i += 4) {
            if (i > 0) sb.append(" ");
            sb.append(clean.substring(i, Math.min(i + 4, clean.length())));
        }
        return sb.toString();
    }

    private String formatReference(String type, String ref) {
        if (ref == null) return "-";
        String clean = ref.replaceAll("\\s+", "");
        if (clean.isEmpty()) return "-";

        if ("QRR".equalsIgnoreCase(type)) {
            StringBuilder sb = new StringBuilder();
            int i = clean.length();
            while (i > 0) {
                if (sb.length() > 0) sb.insert(0, " ");
                sb.insert(0, clean.substring(Math.max(0, i - 5), i));
                i -= 5;
            }
            return sb.toString();
        } else if ("SCOR".equalsIgnoreCase(type)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < clean.length(); i += 4) {
                if (i > 0) sb.append(" ");
                sb.append(clean.substring(i, Math.min(i + 4, clean.length())));
            }
            return sb.toString().toUpperCase();
        }
        return ref;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "";
        DecimalFormat df = new DecimalFormat("#,##0.00");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('\'');
        symbols.setDecimalSeparator('.');
        df.setDecimalFormatSymbols(symbols);
        return df.format(amount);
    }

    private String formatAddressHtml(Address addr, boolean isDebtor) {
        if (addr == null || addr.getName() == null || addr.getName().trim().isEmpty()) {
            return isDebtor ? "<div style=\"border: 1px dashed #bbb; height: 12mm; margin-top: 1mm; width: 100%;\"></div>" : "";
        }
        String name = addr.getName().trim();
        String street = addr.getStreet() != null ? addr.getStreet().trim() : "";
        String houseNo = addr.getHouseNo() != null ? addr.getHouseNo().trim() : "";
        String zip = addr.getPostalCode() != null ? addr.getPostalCode().trim() : "";
        String city = addr.getTown() != null ? addr.getTown().trim() : "";
        String country = addr.getCountryCode() != null ? addr.getCountryCode().trim().toUpperCase() : "";

        if (isDebtor) {
            return String.format("%s<br>%s %s<br>%s-%s %s", name, street, houseNo, country, zip, city);
        } else {
            return String.format("%s<br>%s %s<br>%s %s", name, street, houseNo, zip, city);
        }
    }

    private String maskIban(String iban) {
        if (iban == null) return "null";
        String clean = iban.replaceAll("\\s+", "");
        if (clean.length() <= 8) return clean;
        return clean.substring(0, 8) + " *********";
    }
}
