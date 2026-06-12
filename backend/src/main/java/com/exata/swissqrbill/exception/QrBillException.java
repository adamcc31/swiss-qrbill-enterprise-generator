package com.exata.swissqrbill.exception;

import com.exata.swissqrbill.model.ValidationError;
import java.util.List;

public class QrBillException extends RuntimeException {
    
    private final List<ValidationError> errors;

    public QrBillException(String message) {
        super(message);
        this.errors = List.of();
    }

    public QrBillException(String message, List<ValidationError> errors) {
        super(message);
        this.errors = errors;
    }
    
    public QrBillException(String message, Throwable cause) {
        super(message, cause);
        this.errors = List.of();
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}
