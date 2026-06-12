package com.exata.swissqrbill.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class Address {

    @NotBlank(message = "Name is required.")
    @Size(max = 70, message = "Name must not exceed 70 characters.")
    private String name;

    @NotBlank(message = "Street is required.")
    @Size(max = 70, message = "Street must not exceed 70 characters.")
    private String street;

    @NotBlank(message = "House number is required.")
    @Size(max = 16, message = "House number must not exceed 16 characters.")
    private String houseNo;

    @NotBlank(message = "Postal code is required.")
    @Size(max = 16, message = "Postal code must not exceed 16 characters.")
    private String postalCode;

    @NotBlank(message = "Town is required.")
    @Size(max = 35, message = "Town/City must not exceed 35 characters.")
    private String town;

    @NotBlank(message = "Country code is required.")
    @Size(min = 2, max = 2, message = "Country code must be exactly a 2-letter ISO code.")
    private String countryCode;

    public Address() {}

    public Address(String name, String street, String houseNo, String postalCode, String town, String countryCode) {
        this.name = name;
        this.street = street;
        this.houseNo = houseNo;
        this.postalCode = postalCode;
        this.town = town;
        this.countryCode = countryCode;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getHouseNo() { return houseNo; }
    public void setHouseNo(String houseNo) { this.houseNo = houseNo; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getTown() { return town; }
    public void setTown(String town) { this.town = town; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
}
