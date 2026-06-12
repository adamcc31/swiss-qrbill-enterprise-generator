package com.exata.swissqrbill.controller;

import com.exata.swissqrbill.model.ApiResponse;
import com.exata.swissqrbill.model.QrBillRequest;
import com.exata.swissqrbill.model.QrBillResponse;
import com.exata.swissqrbill.model.ValidationError;
import com.exata.swissqrbill.service.QrBillService;
import com.exata.swissqrbill.service.ValidationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/qrbill")
public class QrBillController {

    private final QrBillService qrBillService;
    private final ValidationService validationService;

    // Enforce constructor injection strictly
    public QrBillController(QrBillService qrBillService, ValidationService validationService) {
        this.qrBillService = qrBillService;
        this.validationService = validationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<QrBillResponse>> generateQrBill(@Valid @RequestBody QrBillRequest request) {
        QrBillResponse response = qrBillService.generateQrBill(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> downloadPdf(@Valid @RequestBody QrBillRequest request) {
        byte[] pdfBytes = qrBillService.generatePdf(request);
        
        String filename = "swiss-qrbill-" + Instant.now().getEpochSecond() + ".pdf";
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<String>> validateQrBill(@RequestBody QrBillRequest request) {
        List<ValidationError> errors = validationService.validateRequest(request);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(errors));
        }
        return ResponseEntity.ok(ApiResponse.success("Valid"));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Healthy"));
    }
}
