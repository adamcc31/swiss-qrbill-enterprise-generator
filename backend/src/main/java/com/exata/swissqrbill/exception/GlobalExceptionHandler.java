package com.exata.swissqrbill.exception;

import com.exata.swissqrbill.model.ApiResponse;
import com.exata.swissqrbill.model.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(QrBillException.class)
    public ResponseEntity<ApiResponse<Void>> handleQrBillException(QrBillException ex) {
        log.warn("Business validation exception: {}", ex.getMessage());
        List<ValidationError> errors = ex.getErrors();
        if (errors == null || errors.isEmpty()) {
            errors = List.of(new ValidationError("validation", ex.getMessage()));
        }
        ApiResponse<Void> response = ApiResponse.error(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.add(new ValidationError(fieldName, errorMessage));
        });
        
        log.warn("Request field validation failed with {} errors", errors.size());
        ApiResponse<Void> response = ApiResponse.error(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("An unexpected error occurred in the system: {}", ex.getMessage(), ex);
        ApiResponse<Void> response = ApiResponse.error("server", "An internal server error occurred. Please contact the administrator.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
