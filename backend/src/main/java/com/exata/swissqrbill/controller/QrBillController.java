package com.exata.swissqrbill.controller;

import com.exata.swissqrbill.model.ApiResponse;
import com.exata.swissqrbill.model.QrBillRequest;
import com.exata.swissqrbill.model.QrBillResponse;
import com.exata.swissqrbill.model.ValidationError;
import com.exata.swissqrbill.service.QrBillService;
import com.exata.swissqrbill.service.ValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/qrbill")
@Tag(name = "Swiss QR-Bill", description = "Endpoints for generating, validating, and downloading Swiss QR-Bills compliant with ISO 20022.")
public class QrBillController {

    private final QrBillService qrBillService;
    private final ValidationService validationService;

    // Enforce constructor injection strictly
    public QrBillController(QrBillService qrBillService, ValidationService validationService) {
        this.qrBillService = qrBillService;
        this.validationService = validationService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate Swiss QR-Bill", description = "Generates a compliant Swiss QR-Bill containing the Swiss QR code matrix and styled payment slip as raw inline SVG.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully generated QR-Bill",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = QrBillResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class)))
    })
    public ResponseEntity<ApiResponse<QrBillResponse>> generateQrBill(@Valid @RequestBody QrBillRequest request) {
        QrBillResponse response = qrBillService.generateQrBill(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/download")
    @Operation(summary = "Download Swiss QR-Bill PDF", description = "Generates and downloads a fully compliant Swiss QR-Bill A4 document in PDF binary format.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully generated PDF binary",
            content = @Content(mediaType = "application/pdf")),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public ResponseEntity<byte[]> downloadPdf(@Valid @RequestBody QrBillRequest request) {
        byte[] pdfBytes = qrBillService.generatePdf(request);
        
        String filename = "swiss-qrbill-" + Instant.now().getEpochSecond() + ".pdf";
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate QR-Bill Details", description = "Validates the correctness of the Swiss QR-Bill details like IBAN correctness, postal code, and payment reference specifications.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payload details are valid",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"success\":true,\"data\":\"Valid\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Payload details are invalid, containing verification errors",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class)))
    })
    public ResponseEntity<ApiResponse<String>> validateQrBill(@RequestBody QrBillRequest request) {
        List<ValidationError> errors = validationService.validateRequest(request);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(errors));
        }
        return ResponseEntity.ok(ApiResponse.success("Valid"));
    }

    @GetMapping("/health")
    @Operation(summary = "API Health Check", description = "Returns the status of the Swiss QR-Bill generation API service.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Service is active and running",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"success\":true,\"data\":\"Healthy\"}")))
    })
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Healthy"));
    }
}
