package com.exata.swissqrbill.controller;

import com.exata.swissqrbill.exception.QrBillException;
import com.exata.swissqrbill.model.*;
import com.exata.swissqrbill.service.QrBillService;
import com.exata.swissqrbill.service.ValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QrBillController.class)
public class QrBillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QrBillService qrBillService;

    @MockBean
    private ValidationService validationService;

    private QrBillRequest createValidBaseRequest() {
        QrBillRequest request = new QrBillRequest();
        
        Address creditor = new Address();
        creditor.setName("Exata Indonesia AG");
        creditor.setStreet("Bahnhofstrasse");
        creditor.setHouseNo("1");
        creditor.setPostalCode("8001");
        creditor.setTown("Zürich");
        creditor.setCountryCode("CH");
        request.setCreditorAddress(creditor);
        
        request.setCreditorIban("CH5604835012345678009");
        request.setCurrency("CHF");
        
        PaymentReference ref = new PaymentReference();
        ref.setReferenceType("SCOR");
        ref.setReferenceNumber("RF18539007547034");
        request.setReference(ref);
        
        return request;
    }

    @Test
    public void generateQrBill_success() throws Exception {
        QrBillResponse mockResponse = new QrBillResponse(
                "<svg>QR</svg>", "CH56 0483 5012 3456 7800 9", "RF18 5390 0754 7034",
                "1'250.00", "Creditor Address", "Debtor Address", "CHF", "Invoice 1042", true
        );

        when(qrBillService.generateQrBill(any(QrBillRequest.class))).thenReturn(mockResponse);

        QrBillRequest request = createValidBaseRequest();

        mockMvc.perform(post("/api/v1/qrbill/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.qrImage").value("<svg>QR</svg>"))
                .andExpect(jsonPath("$.data.formattedCreditorIban").value("CH56 0483 5012 3456 7800 9"));
    }

    @Test
    public void generateQrBill_businessValidationError() throws Exception {
        ValidationError error = new ValidationError("creditorIban", "Invalid IBAN");
        when(qrBillService.generateQrBill(any(QrBillRequest.class)))
                .thenThrow(new QrBillException("Validation failed", List.of(error)));

        QrBillRequest request = createValidBaseRequest();

        mockMvc.perform(post("/api/v1/qrbill/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].field").value("creditorIban"))
                .andExpect(jsonPath("$.errors[0].message").value("Invalid IBAN"));
    }

    @Test
    public void generateQrBill_fieldValidationError() throws Exception {
        QrBillRequest request = createValidBaseRequest();
        request.setCreditorIban(""); // Triggers @NotBlank

        mockMvc.perform(post("/api/v1/qrbill/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].field").value("creditorIban"))
                .andExpect(jsonPath("$.errors[0].message").value("Creditor IBAN is required."));
    }

    @Test
    public void generateQrBill_unexpectedError() throws Exception {
        when(qrBillService.generateQrBill(any(QrBillRequest.class)))
                .thenThrow(new RuntimeException("Unexpected database failure"));

        QrBillRequest request = createValidBaseRequest();

        mockMvc.perform(post("/api/v1/qrbill/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].field").value("server"))
                .andExpect(jsonPath("$.errors[0].message").value("An internal server error occurred. Please contact the administrator."));
    }

    @Test
    public void downloadPdf_success() throws Exception {
        byte[] pdfContent = new byte[]{1, 2, 3, 4};
        when(qrBillService.generatePdf(any(QrBillRequest.class))).thenReturn(pdfContent);

        QrBillRequest request = createValidBaseRequest();

        mockMvc.perform(post("/api/v1/qrbill/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    public void validateQrBill_valid() throws Exception {
        when(validationService.validateRequest(any(QrBillRequest.class))).thenReturn(List.of());

        QrBillRequest request = createValidBaseRequest();

        mockMvc.perform(post("/api/v1/qrbill/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Valid"));
    }

    @Test
    public void validateQrBill_invalid() throws Exception {
        ValidationError error = new ValidationError("amount", "Amount must be positive");
        when(validationService.validateRequest(any(QrBillRequest.class))).thenReturn(List.of(error));

        QrBillRequest request = createValidBaseRequest();

        mockMvc.perform(post("/api/v1/qrbill/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].field").value("amount"))
                .andExpect(jsonPath("$.errors[0].message").value("Amount must be positive"));
    }

    @Test
    public void healthCheck_success() throws Exception {
        mockMvc.perform(get("/api/v1/qrbill/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Healthy"));
    }

    @Test
    public void testQrBillExceptionConstructor_messageAndCause() {
        QrBillException ex1 = new QrBillException("Error message");
        assertEquals("Error message", ex1.getMessage());
        assertTrue(ex1.getErrors().isEmpty());

        RuntimeException cause = new RuntimeException("Underlying cause");
        QrBillException ex2 = new QrBillException("Error with cause", cause);
        assertEquals("Error with cause", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
        assertTrue(ex2.getErrors().isEmpty());
    }
}
