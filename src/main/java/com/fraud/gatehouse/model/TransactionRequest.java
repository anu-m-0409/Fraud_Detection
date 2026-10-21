package com.fraud.gatehouse.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public class TransactionRequest {
    @NotBlank(message = "userId must not be blank")
    private String userId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    private Double amount;

    @NotBlank(message = "merchantCategory is required")
    private String merchantCategory;

    @NotBlank(message = "country is required")
    private String country;

    @NotBlank(message = "deviceId is required")
    private String deviceId;

    private Double distanceFromHomeKm = 0.0;
    private OffsetDateTime timestamp;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public Double getDistanceFromHomeKm() { return distanceFromHomeKm != null ? distanceFromHomeKm : 0.0; }
    public void setDistanceFromHomeKm(Double distanceFromHomeKm) { this.distanceFromHomeKm = distanceFromHomeKm; }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
}
