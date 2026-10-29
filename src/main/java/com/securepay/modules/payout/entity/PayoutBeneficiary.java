package com.securepay.modules.payout.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * {@code payout_beneficiaries} table (PayoutBeneficiary). {@code type} is BANK / UPI for the
 * ImpsGuru flow and SOLITECK / ABHAYAM / UTKAL / UTKALUPI for provider-managed beneficiaries.
 */
@Entity
@Table(name = "payout_beneficiaries")
public class PayoutBeneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("sender_id")
    @Column(name = "sender_id")
    private Long senderId;

    @JsonProperty("beneficiary_name")
    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @JsonProperty("account_number")
    @Column(name = "account_number")
    private String accountNumber;

    @JsonProperty("ifsc_code")
    @Column(name = "ifsc_code")
    private String ifscCode;

    @JsonProperty("bank_name")
    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "type")
    private String type;

    @JsonProperty("soliteck_id")
    @Column(name = "soliteck_id")
    private String soliteckId;

    @JsonProperty("api_id")
    @Column(name = "api_id")
    private String apiId;

    @JsonProperty("mobile_number")
    @Column(name = "mobile_number")
    private String mobileNumber;

    @JsonProperty("is_verified")
    @Column(name = "is_verified")
    private Integer isVerified;

    @JsonProperty("is_active")
    @Column(name = "is_active")
    private Integer isActive;

    @JsonProperty("created_at")
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void defaults() {
        if (isVerified == null) isVerified = 0;
        if (isActive == null) isActive = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSoliteckId() { return soliteckId; }
    public void setSoliteckId(String soliteckId) { this.soliteckId = soliteckId; }
    public String getApiId() { return apiId; }
    public void setApiId(String apiId) { this.apiId = apiId; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public Integer getIsVerified() { return isVerified; }
    public void setIsVerified(Integer isVerified) { this.isVerified = isVerified; }
    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
