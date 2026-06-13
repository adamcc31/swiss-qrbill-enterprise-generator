package com.exata.swissqrbill.service;

import com.exata.swissqrbill.model.Address;
import com.exata.swissqrbill.model.PaymentReference;
import com.exata.swissqrbill.model.QrBillRequest;
import com.exata.swissqrbill.model.ValidationError;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationServiceTest {

    private final ValidationService validationService = new ValidationService();

    @Test
    public void validateRequest_nullRequest() {
        List<ValidationError> errors = validationService.validateRequest(null);
        assertFalse(errors.isEmpty());
        assertEquals("request", errors.get(0).field());
    }

    @Test
    public void validateRequest_nullCreditorAddress() {
        QrBillRequest request = new QrBillRequest();
        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "creditorAddress".equals(e.field())));
    }

    @Test
    public void validateRequest_invalidCreditorAddress() {
        QrBillRequest request = new QrBillRequest();
        Address address = new Address();
        address.setName("");
        address.setStreet("Bahnhofstrasse");
        address.setHouseNo("1");
        address.setPostalCode("8001");
        address.setTown("Zürich");
        address.setCountryCode("CH");
        request.setCreditorAddress(address);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "creditorAddress.name".equals(e.field())));
    }

    @Test
    public void validateRequest_nullIban() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban(null);
        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "creditorIban".equals(e.field())));
    }

    @Test
    public void validateRequest_emptyIban() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("   ");
        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "creditorIban".equals(e.field())));
    }

    @Test
    public void validateRequest_invalidIbanChecksum() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5604835012345678008"); // Invalid checksum (ends with 8 instead of 9)
        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "creditorIban".equals(e.field())));
    }

    @Test
    public void validateRequest_wrongIbanLength() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH56048350123456780"); // Too short
        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "creditorIban".equals(e.field())));
    }

    @Test
    public void validateRequest_amountZero() {
        QrBillRequest request = createValidBaseRequest();
        request.setAmount(BigDecimal.ZERO);
        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "amount".equals(e.field())));
    }

    @Test
    public void validateRequest_amountNegative() {
        QrBillRequest request = createValidBaseRequest();
        request.setAmount(new BigDecimal("-10.00"));
        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "amount".equals(e.field())));
    }

    @Test
    public void validateRequest_debtorNameWithoutAddress() {
        QrBillRequest request = createValidBaseRequest();
        Address debtor = new Address();
        debtor.setName("Test Debtor");
        request.setDebtorAddress(debtor);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "debtorAddress.street".equals(e.field())));
    }

    @Test
    public void validateRequest_debtorAddressInvalid() {
        QrBillRequest request = createValidBaseRequest();
        Address debtor = new Address();
        debtor.setName("Test Debtor");
        debtor.setStreet("Street");
        debtor.setHouseNo("2");
        debtor.setPostalCode("3000");
        debtor.setTown("Bern");
        debtor.setCountryCode("USA"); // Not 2 letters
        request.setDebtorAddress(debtor);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "debtorAddress.countryCode".equals(e.field())));
    }

    @Test
    public void validateRequest_nullReference() {
        QrBillRequest request = createValidBaseRequest();
        request.setReference(null);
        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "reference.referenceType".equals(e.field())));
    }

    @Test
    public void validateRequest_qrrWithoutQrIban() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5604835012345678009"); // standard IBAN
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("QRR");
        ref.setReferenceNumber("210000000003113920000057214");
        request.setReference(ref);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "creditorIban".equals(e.field())));
    }

    @Test
    public void validateRequest_qrrWithEmptyRefNumber() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5730000123456789012"); // QR-IBAN
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("QRR");
        ref.setReferenceNumber("");
        request.setReference(ref);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "reference.referenceNumber".equals(e.field())));
    }

    @Test
    public void validateRequest_qrrWithInvalidChecksum() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5730000123456789012"); // QR-IBAN
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("QRR");
        ref.setReferenceNumber("210000000003113920000057215"); // Check digit 5 (invalid, should be 4)
        request.setReference(ref);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "reference.referenceNumber".equals(e.field())));
    }

    @Test
    public void validateRequest_qrrWithNonNumeric() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5730000123456789012"); // QR-IBAN
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("QRR");
        ref.setReferenceNumber("21000000000311392000005721A"); // has 'A'
        request.setReference(ref);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "reference.referenceNumber".equals(e.field())));
    }

    @Test
    public void validateRequest_qrrWithWrongLength() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5730000123456789012"); // QR-IBAN
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("QRR");
        ref.setReferenceNumber("2100000000031139200000572"); // Too short
        request.setReference(ref);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "reference.referenceNumber".equals(e.field())));
    }

    @Test
    public void validateRequest_qrrValid() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5730000123456789012"); // QR-IBAN
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("QRR");
        ref.setReferenceNumber("210000000003113920000057214");
        request.setReference(ref);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void validateRequest_scorWithQrIban() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5730000123456789012"); // QR-IBAN
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("SCOR");
        ref.setReferenceNumber("RF18539007547034");
        request.setReference(ref);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "creditorIban".equals(e.field())));
    }

    @Test
    public void validateRequest_scorWithEmptyRefNumber() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5604835012345678009"); // Standard IBAN
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("SCOR");
        ref.setReferenceNumber("");
        request.setReference(ref);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "reference.referenceNumber".equals(e.field())));
    }

    @Test
    public void validateRequest_scorWithInvalidChecksum() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5604835012345678009"); // Standard IBAN
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("SCOR");
        ref.setReferenceNumber("RF18539007547035"); // Invalid checksum
        request.setReference(ref);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "reference.referenceNumber".equals(e.field())));
    }

    @Test
    public void validateRequest_scorWithWrongPrefix() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5604835012345678009"); // Standard IBAN
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("SCOR");
        ref.setReferenceNumber("AB18539007547034"); // Wrong prefix
        request.setReference(ref);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "reference.referenceNumber".equals(e.field())));
    }

    @Test
    public void validateRequest_scorValid() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5604835012345678009"); // Standard IBAN
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("SCOR");
        ref.setReferenceNumber("RF18539007547034");
        request.setReference(ref);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void validateRequest_nonWithQrIban() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5730000123456789012"); // QR-IBAN
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("NON");
        ref.setReferenceNumber(null);
        request.setReference(ref);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "creditorIban".equals(e.field())));
    }

    @Test
    public void validateRequest_nonWithNotEmptyRefNumber() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5604835012345678009"); // Standard IBAN
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("NON");
        ref.setReferenceNumber("RF18539007547034"); // should be empty
        request.setReference(ref);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.stream().anyMatch(e -> "reference.referenceNumber".equals(e.field())));
    }

    @Test
    public void validateRequest_nonValid() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5604835012345678009"); // Standard IBAN
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("NON");
        ref.setReferenceNumber(null);
        request.setReference(ref);

        List<ValidationError> errors = validationService.validateRequest(request);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void isQRIban_valid() {
        assertTrue(validationService.isQRIban("CH5730000123456789012"));
    }

    @Test
    public void isQRIban_invalid() {
        assertFalse(validationService.isQRIban("CH5604835012345678009"));
        assertFalse(validationService.isQRIban(null));
        assertFalse(validationService.isQRIban("CH560483"));
        assertFalse(validationService.isQRIban("CH56X4835012345678009"));
    }

    @Test
    public void calculateModulo10Recursive_invalidChar() {
        assertThrows(IllegalArgumentException.class, () -> {
            validationService.calculateModulo10Recursive("123A5");
        });
    }

    private QrBillRequest createValidBaseRequest() {
        QrBillRequest request = new QrBillRequest();
        
        Address creditor = new Address();
        creditor.setName("Helvetia AG");
        creditor.setStreet("Bahnhofstrasse");
        creditor.setHouseNo("1");
        creditor.setPostalCode("8001");
        creditor.setTown("Zürich");
        creditor.setCountryCode("CH");
        request.setCreditorAddress(creditor);
        
        request.setCreditorIban("CH5604835012345678009");
        request.setAmount(new BigDecimal("1250.00"));
        request.setCurrency("CHF");
        
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("SCOR");
        ref.setReferenceNumber("RF18539007547034");
        request.setReference(ref);
        
        return request;
    }
}
