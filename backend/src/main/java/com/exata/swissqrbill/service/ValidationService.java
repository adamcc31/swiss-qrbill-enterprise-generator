package com.exata.swissqrbill.service;

import com.exata.swissqrbill.model.Address;
import com.exata.swissqrbill.model.QrBillRequest;
import com.exata.swissqrbill.model.ValidationError;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Service
public class ValidationService {

    /**
     * Performs full compliance validations based on SIX Group Swiss Payment Standards.
     */
    public List<ValidationError> validateRequest(QrBillRequest request) {
        List<ValidationError> errors = new ArrayList<>();

        if (request == null) {
            errors.add(new ValidationError("request", "Request body cannot be null."));
            return errors;
        }

        // 1. Validate Creditor Address (Structured S type)
        Address creditor = request.getCreditorAddress();
        if (creditor == null) {
            errors.add(new ValidationError("creditorAddress", "Creditor address is required."));
        } else {
            validateAddress(creditor, "creditorAddress", errors);
        }

        // 2. Validate Creditor IBAN
        String rawIban = request.getCreditorIban();
        boolean isIbanValid = false;
        if (rawIban == null || rawIban.trim().isEmpty()) {
            errors.add(new ValidationError("creditorIban", "Creditor IBAN is required."));
        } else {
            String cleanIban = rawIban.replaceAll("\\s+", "").toUpperCase();
            if (!validateIBANChecksum(cleanIban)) {
                errors.add(new ValidationError("creditorIban", "Invalid Swiss/Liechtenstein IBAN checksum."));
            } else {
                isIbanValid = true;
            }
        }

        // 3. Validate Amount & Currency
        BigDecimal amount = request.getAmount();
        if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new ValidationError("amount", "Amount must be greater than 0."));
        }

        // 4. Validate Debtor Address (Conditional: if Name is filled, address is mandatory)
        Address debtor = request.getDebtorAddress();
        if (debtor != null && debtor.getName() != null && !debtor.getName().trim().isEmpty()) {
            validateAddress(debtor, "debtorAddress", errors);
        }

        // 5. Reference & IBAN Compliance Cross-Checks
        if (request.getReference() == null || request.getReference().getReferenceType() == null) {
            errors.add(new ValidationError("reference.referenceType", "Reference type is required."));
        } else if (isIbanValid) {
            String refType = request.getReference().getReferenceType().trim().toUpperCase();
            String refNum = request.getReference().getReferenceNumber();
            String cleanRef = refNum != null ? refNum.replaceAll("\\s+", "") : "";
            boolean isQrIbanVal = isQRIban(rawIban);

            if ("QRR".equals(refType)) {
                // Rule: QRR must use a QR-IBAN
                if (!isQrIbanVal) {
                    errors.add(new ValidationError("creditorIban", "QR-Reference requires a QR-IBAN (IID 30000-31999)."));
                }
                // Rule: QR-Reference must be valid
                if (cleanRef.isEmpty()) {
                    errors.add(new ValidationError("reference.referenceNumber", "Reference number is required for QRR."));
                } else if (!validateQRReference(cleanRef)) {
                    errors.add(new ValidationError("reference.referenceNumber", "Invalid QR-Reference checksum (27 digits, Mod 10 recursive)."));
                }
            } else if ("SCOR".equals(refType)) {
                // Rule: SCOR must use standard IBAN (not QR-IBAN)
                if (isQrIbanVal) {
                    errors.add(new ValidationError("creditorIban", "SCOR reference requires a standard IBAN (not a QR-IBAN)."));
                }
                // Rule: SCOR must be valid ISO 11649
                if (cleanRef.isEmpty()) {
                    errors.add(new ValidationError("reference.referenceNumber", "Reference number is required for SCOR."));
                } else if (!validateSCORReference(cleanRef)) {
                    errors.add(new ValidationError("reference.referenceNumber", "Invalid SCOR reference checksum (ISO 11649, e.g. RFxx...)."));
                }
            } else if ("NON".equals(refType)) {
                // Rule: NON must use standard IBAN (not QR-IBAN)
                if (isQrIbanVal) {
                    errors.add(new ValidationError("creditorIban", "No Reference (NON) requires a standard IBAN (not a QR-IBAN)."));
                }
                if (!cleanRef.isEmpty()) {
                    errors.add(new ValidationError("reference.referenceNumber", "Reference number must be empty for NON type."));
                }
            } else {
                errors.add(new ValidationError("reference.referenceType", "Unknown reference type: " + refType));
            }
        }

        return errors;
    }

    private void validateAddress(Address address, String pathPrefix, List<ValidationError> errors) {
        if (isEmpty(address.getName())) {
            errors.add(new ValidationError(pathPrefix + ".name", "Name is required."));
        }
        if (isEmpty(address.getStreet())) {
            errors.add(new ValidationError(pathPrefix + ".street", "Street is required."));
        }
        if (isEmpty(address.getHouseNo())) {
            errors.add(new ValidationError(pathPrefix + ".houseNo", "House number is required."));
        }
        if (isEmpty(address.getPostalCode())) {
            errors.add(new ValidationError(pathPrefix + ".postalCode", "Postal code/ZIP is required."));
        }
        if (isEmpty(address.getTown())) {
            errors.add(new ValidationError(pathPrefix + ".town", "Town/City is required."));
        }
        String country = address.getCountryCode();
        if (isEmpty(country) || country.trim().length() != 2) {
            errors.add(new ValidationError(pathPrefix + ".countryCode", "Country code must be exactly a 2-letter ISO code."));
        }
    }

    private boolean isEmpty(String val) {
        return val == null || val.trim().isEmpty();
    }

    /**
     * ISO 7064 Modulo 97-10 checksum validation (for IBAN)
     */
    public boolean validateIBANChecksum(String iban) {
        if (iban == null) return false;
        String cleanIban = iban.replaceAll("\\s+", "").toUpperCase();
        if (cleanIban.length() != 21) return false;
        if (!cleanIban.startsWith("CH") && !cleanIban.startsWith("LI")) return false;

        String rearranged = cleanIban.substring(4) + cleanIban.substring(0, 4);

        StringBuilder numeric = new StringBuilder();
        for (int i = 0; i < rearranged.length(); i++) {
            char ch = rearranged.charAt(i);
            if (Character.isLetter(ch)) {
                numeric.append(ch - 'A' + 10);
            } else if (Character.isDigit(ch)) {
                numeric.append(ch);
            } else {
                return false;
            }
        }

        try {
            BigInteger bigNumber = new BigInteger(numeric.toString());
            return bigNumber.mod(BigInteger.valueOf(97)).intValue() == 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Determine if an IBAN is a QR-IBAN (IID range 30000 - 31999)
     */
    public boolean isQRIban(String iban) {
        if (iban == null) return false;
        String clean = iban.replaceAll("\\s+", "");
        if (clean.length() != 21) return false;
        try {
            int iid = Integer.parseInt(clean.substring(4, 9));
            return iid >= 30000 && iid <= 31999;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Modulo 10 recursive checksum (Luhn-like algorithm for Swiss ISR/QRR references)
     */
    public int calculateModulo10Recursive(String reference) {
        int[] table = {0, 9, 4, 6, 8, 2, 7, 1, 3, 5};
        int carry = 0;
        for (int i = 0; i < reference.length(); i++) {
            int digit = Character.getNumericValue(reference.charAt(i));
            if (digit < 0 || digit > 9) {
                throw new IllegalArgumentException("Reference must contain only digits for Mod 10.");
            }
            carry = table[(carry + digit) % 10];
        }
        return (10 - carry) % 10;
    }

    /**
     * Validate QR-Reference format and checksum
     */
    public boolean validateQRReference(String ref) {
        if (ref == null) return false;
        String clean = ref.replaceAll("\\s+", "");
        if (clean.length() != 27) return false;
        if (!clean.matches("^\\d+$")) return false;

        String payload = clean.substring(0, 26);
        int checkDigit = Character.getNumericValue(clean.charAt(26));
        try {
            return calculateModulo10Recursive(payload) == checkDigit;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validate SCOR (ISO 11649 Structured Creditor Reference)
     */
    public boolean validateSCORReference(String ref) {
        if (ref == null) return false;
        String clean = ref.replaceAll("\\s+", "").toUpperCase();
        if (clean.length() < 5 || clean.length() > 25) return false;
        if (!clean.startsWith("RF")) return false;
        if (!clean.substring(2, 4).matches("^\\d+$")) return false;

        String rearranged = clean.substring(4) + clean.substring(0, 4);

        StringBuilder numeric = new StringBuilder();
        for (int i = 0; i < rearranged.length(); i++) {
            char ch = rearranged.charAt(i);
            if (Character.isLetter(ch)) {
                numeric.append(ch - 'A' + 10);
            } else if (Character.isDigit(ch)) {
                numeric.append(ch);
            } else {
                return false;
            }
        }

        try {
            BigInteger bigNumber = new BigInteger(numeric.toString());
            return bigNumber.mod(BigInteger.valueOf(97)).intValue() == 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
