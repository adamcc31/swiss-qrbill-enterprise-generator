package com.exata.swissqrbill.model;

public class QrBillResponse {

    private String qrImage;
    private String formattedCreditorIban;
    private String formattedReferenceNumber;
    private String formattedAmount;
    private String creditorAddressHtml;
    private String debtorAddressHtml;
    private String currency;
    private String message;
    private boolean hasAmount;

    public QrBillResponse() {}

    public QrBillResponse(String qrImage, String formattedCreditorIban, String formattedReferenceNumber,
                          String formattedAmount, String creditorAddressHtml, String debtorAddressHtml,
                          String currency, String message, boolean hasAmount) {
        this.qrImage = qrImage;
        this.formattedCreditorIban = formattedCreditorIban;
        this.formattedReferenceNumber = formattedReferenceNumber;
        this.formattedAmount = formattedAmount;
        this.creditorAddressHtml = creditorAddressHtml;
        this.debtorAddressHtml = debtorAddressHtml;
        this.currency = currency;
        this.message = message;
        this.hasAmount = hasAmount;
    }

    // Getters and Setters
    public String getQrImage() { return qrImage; }
    public void setQrImage(String qrImage) { this.qrImage = qrImage; }
    public String getFormattedCreditorIban() { return formattedCreditorIban; }
    public void setFormattedCreditorIban(String formattedCreditorIban) { this.formattedCreditorIban = formattedCreditorIban; }
    public String getFormattedReferenceNumber() { return formattedReferenceNumber; }
    public void setFormattedReferenceNumber(String formattedReferenceNumber) { this.formattedReferenceNumber = formattedReferenceNumber; }
    public String getFormattedAmount() { return formattedAmount; }
    public void setFormattedAmount(String formattedAmount) { this.formattedAmount = formattedAmount; }
    public String getCreditorAddressHtml() { return creditorAddressHtml; }
    public void setCreditorAddressHtml(String creditorAddressHtml) { this.creditorAddressHtml = creditorAddressHtml; }
    public String getDebtorAddressHtml() { return debtorAddressHtml; }
    public void setDebtorAddressHtml(String debtorAddressHtml) { this.debtorAddressHtml = debtorAddressHtml; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isHasAmount() { return hasAmount; }
    public void setHasAmount(boolean hasAmount) { this.hasAmount = hasAmount; }
}
