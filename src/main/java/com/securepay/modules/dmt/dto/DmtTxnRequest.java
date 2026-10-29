package com.securepay.modules.dmt.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

/**
 * Inbound fields for a DMT transaction (decrypted payload), mirroring the request fields read by
 * {@code DmtService::create} (doc 10 §10.4). Payload is snake_case (mobile_number, ben_name, …).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DmtTxnRequest {

    private BigDecimal amount;
    private String clientRef;
    private String mobileNumber;
    private String benName;
    private String benAcc;
    private String ifsc;
    private String bankName;
    private String type;
    private String latitude;
    private String longitude;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getClientRef() {
        return clientRef;
    }

    public void setClientRef(String clientRef) {
        this.clientRef = clientRef;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getBenName() {
        return benName;
    }

    public void setBenName(String benName) {
        this.benName = benName;
    }

    public String getBenAcc() {
        return benAcc;
    }

    public void setBenAcc(String benAcc) {
        this.benAcc = benAcc;
    }

    public String getIfsc() {
        return ifsc;
    }

    public void setIfsc(String ifsc) {
        this.ifsc = ifsc;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
}
