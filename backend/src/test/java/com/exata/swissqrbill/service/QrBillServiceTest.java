package com.exata.swissqrbill.service;

import com.exata.swissqrbill.exception.QrBillException;
import com.exata.swissqrbill.model.Address;
import com.exata.swissqrbill.model.PaymentReference;
import com.exata.swissqrbill.model.QrBillRequest;
import com.exata.swissqrbill.model.QrBillResponse;
import com.exata.swissqrbill.model.ValidationError;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class QrBillServiceTest {

    private final ValidationService validationService = new ValidationService();
    private final QrBillService qrBillService = new QrBillService(validationService);

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
        request.setMessage("Invoice 1042");
        
        return request;
    }

    @Test
    public void generateQrBill_success() {
        QrBillRequest request = createValidBaseRequest();
        QrBillResponse response = qrBillService.generateQrBill(request);
        
        assertNotNull(response);
        assertNotNull(response.getQrImage());
        assertTrue(response.getQrImage().contains("<svg"));
        assertEquals("CH56 0483 5012 3456 7800 9", response.getFormattedCreditorIban());
        assertEquals("RF18 5390 0754 7034", response.getFormattedReferenceNumber());
        assertEquals("1'250.00", response.getFormattedAmount());
        assertEquals("CHF", response.getCurrency());
        assertEquals("Invoice 1042", response.getMessage());
        assertTrue(response.isHasAmount());
        assertTrue(response.getCreditorAddressHtml().contains("Helvetia AG"));
    }

    @Test
    public void generateQrBill_successWithDebtorAndQRR() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("CH5730000123456789012"); // QR-IBAN
        request.getReference().setReferenceType("QRR");
        request.getReference().setReferenceNumber("210000000003113920000057214");

        Address debtor = new Address();
        debtor.setName("Debtor Customer");
        debtor.setStreet("Streetname");
        debtor.setHouseNo("4");
        debtor.setPostalCode("3000");
        debtor.setTown("Bern");
        debtor.setCountryCode("CH");
        request.setDebtorAddress(debtor);

        QrBillResponse response = qrBillService.generateQrBill(request);
        
        assertNotNull(response);
        assertNotNull(response.getQrImage());
        assertEquals("CH57 3000 0123 4567 8901 2", response.getFormattedCreditorIban());
        assertEquals("21 00000 00003 11392 00000 57214", response.getFormattedReferenceNumber());
        assertTrue(response.getDebtorAddressHtml().contains("Debtor Customer"));
    }

    @Test
    public void generateQrBill_successNoAmountNoMessageNoDebtorNonRef() {
        QrBillRequest request = createValidBaseRequest();
        request.setAmount(null);
        request.setMessage(null);
        request.setDebtorAddress(null);
        request.getReference().setReferenceType("NON");
        request.getReference().setReferenceNumber(null);

        QrBillResponse response = qrBillService.generateQrBill(request);
        
        assertNotNull(response);
        assertEquals("", response.getFormattedAmount());
        assertNull(response.getMessage());
        assertFalse(response.isHasAmount());
        assertEquals("-", response.getFormattedReferenceNumber());
    }

    @Test
    public void generateQrBill_validationFailed() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("INVALID_IBAN");

        QrBillException exception = assertThrows(QrBillException.class, () -> {
            qrBillService.generateQrBill(request);
        });

        assertNotNull(exception.getErrors());
        assertFalse(exception.getErrors().isEmpty());
        assertTrue(exception.getMessage().contains("Validation failed"));
    }

    @Test
    public void generateQrBill_unexpectedLibraryError() {
        // Use test double ValidationService to return success but trigger error in buildBill/QRBill
        ValidationService mockValidation = new BypassValidationService();

        QrBillService serviceWithMock = new QrBillService(mockValidation);

        QrBillRequest request = createValidBaseRequest();
        // Cause standard QRBill.generate to crash by passing null values in Address that are bypassed by mock validation
        request.getCreditorAddress().setName(null);

        QrBillException exception = assertThrows(QrBillException.class, () -> {
            serviceWithMock.generateQrBill(request);
        });

        assertTrue(exception.getMessage().contains("Swiss payment standards") || exception.getMessage().contains("internal server error"));
    }

    @Test
    public void generatePdf_success() {
        QrBillRequest request = createValidBaseRequest();
        byte[] pdfBytes = qrBillService.generatePdf(request);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    public void generatePdf_validationFailed() {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban("INVALID_IBAN");

        QrBillException exception = assertThrows(QrBillException.class, () -> {
            qrBillService.generatePdf(request);
        });

        assertNotNull(exception.getErrors());
        assertFalse(exception.getErrors().isEmpty());
    }

    @Test
    public void generatePdf_unexpectedLibraryError() {
        ValidationService mockValidation = new BypassValidationService();

        QrBillService serviceWithMock = new QrBillService(mockValidation);

        QrBillRequest request = createValidBaseRequest();
        request.getCreditorAddress().setName(null);

        QrBillException exception = assertThrows(QrBillException.class, () -> {
            serviceWithMock.generatePdf(request);
        });

        assertTrue(exception.getMessage().contains("Swiss payment standards") || exception.getMessage().contains("internal server error"));
    }

    @Test
    public void testMaskIban_nullAndShort() {
        // Using mock reflection or calling generateQrBill with specific IBANs to hit the private maskIban
        // Let's call the public endpoints which trigger maskIban in logging
        QrBillRequest request = createValidBaseRequest();
        
        // Let's test standard log flow with valid request
        assertNotNull(qrBillService.generateQrBill(request));

        // Short IBAN path (less than 8 chars) will trigger QRBillValidationError internally, but let's see if we can trigger it:
        // Let's test with test double validation service to bypass validation and trigger maskIban with short/null values
        ValidationService mockValidation = new BypassValidationService();
        QrBillService serviceWithMock = new QrBillService(mockValidation);

        QrBillRequest requestShortIban = createValidBaseRequest();
        requestShortIban.setCreditorIban("CH12"); // < 8 characters
        assertThrows(QrBillException.class, () -> {
            serviceWithMock.generateQrBill(requestShortIban);
        });

        QrBillRequest requestNullIban = createValidBaseRequest();
        requestNullIban.setCreditorIban(null);
        assertThrows(QrBillException.class, () -> {
            serviceWithMock.generateQrBill(requestNullIban);
        });
    }

    private static class BypassValidationService extends ValidationService {
        @Override
        public List<ValidationError> validateRequest(QrBillRequest request) {
            return Collections.emptyList();
        }
    }
}
