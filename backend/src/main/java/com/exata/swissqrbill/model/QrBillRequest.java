package com.exata.swissqrbill.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class QrBillRequest {

    @NotNull(message = "Creditor address is required.")
    @Valid
    private Address creditorAddress;

    @NotBlank(message = "Creditor IBAN is required.")
    private String creditorIban;

    private BigDecimal amount;

    @NotBlank(message = "Currency is required.")
    @Pattern(regexp = "^(CHF|EUR)$", message = "Currency must be CHF or EUR.")
    private String currency;

    private Address debtorAddress;

    @NotNull(message = "Reference is required.")
    @Valid
    private PaymentReference reference;

    @Size(max = 140, message = "Message must not exceed 140 characters.")
    private String message;

    public QrBillRequest() {}

    public QrBillRequest(Address creditorAddress, String creditorIban, BigDecimal amount, String currency,
                         Address debtorAddress, PaymentReference reference, String message) {
        this.creditorAddress = creditorAddress;
        this.creditorIban = creditorIban;
        this.amount = amount;
        this.currency = currency;
        this.debtorAddress = debtorAddress;
        this.reference = reference;
        this.message = message;
    }

    // Getters and Setters
    public Address getCreditorAddress() { return creditorAddress; }
    public void setCreditorAddress(Address creditorAddress) { this.creditorAddress = creditorAddress; }
    public String getCreditorIban() { return creditorIban; }
    public void setCreditorIban(String creditorIban) { this.creditorIban = creditorIban; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Address getDebtorAddress() { return debtorAddress; }
    public void setDebtorAddress(Address debtorAddress) { this.debtorAddress = debtorAddress; }
    public PaymentReference getReference() { return reference; }
    public void setReference(PaymentReference reference) { this.reference = reference; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
