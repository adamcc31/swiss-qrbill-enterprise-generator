package com.exata.swissqrbill.model;

import jakarta.validation.constraints.NotBlank;

public class PaymentReference {

    @NotBlank(message = "Reference type is required.")
    private String referenceType;
    
    private String referenceNumber;

    public PaymentReference() {}

    public PaymentReference(String referenceType, String referenceNumber) {
        this.referenceType = referenceType;
        this.referenceNumber = referenceNumber;
    }

    // Getters and Setters
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
}
