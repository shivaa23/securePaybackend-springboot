package com.securepay.modules.payout.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code payout_senders} table (App\Modules\Payout\Models\PayoutSender). A sender (remitter) is
 * identified per provider pipe (SOLITECK / ABHAYAM / UTKAL / null for the legacy ImpsGuru flow).
 * Beneficiaries relate to a sender by {@code mobile_number} (not by id), as in the source.
 */
@Entity
@Table(name = "payout_senders")
public class PayoutSender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("user_id")
    @Column(name = "user_id")
    private Long userId;

    @JsonProperty("sender_name")
    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "email")
    private String email;

    @JsonProperty("mobile_number")
    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "pipe")
    private String pipe;

    @JsonProperty("kyc_status")
    @Column(name = "kyc_status")
    private String kycStatus;

    @JsonProperty("rem_limit")
    @Column(name = "rem_limit", precision = 20, scale = 2)
    private BigDecimal remLimit;

    @JsonProperty("is_active")
    @Column(name = "is_active")
    private Integer isActive;

    @Column(name = "aadhar")
    private String aadhar;

    @Column(name = "pan")
    private String pan;

    @JsonProperty("created_at")
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Loaded relation (hasMany by mobile_number) — serialised as "beneficiary" like Eloquent. */
    @Transient
    @JsonProperty("beneficiary")
    private List<PayoutBeneficiary> beneficiary;

    @PrePersist
    void defaults() {
        if (kycStatus == null) kycStatus = "pending";
        if (isActive == null) isActive = 0;
        if (remLimit == null) remLimit = new BigDecimal("200000");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public String getPipe() { return pipe; }
    public void setPipe(String pipe) { this.pipe = pipe; }
    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }
    public BigDecimal getRemLimit() { return remLimit; }
    public void setRemLimit(BigDecimal remLimit) { this.remLimit = remLimit; }
    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }
    public String getAadhar() { return aadhar; }
    public void setAadhar(String aadhar) { this.aadhar = aadhar; }
    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<PayoutBeneficiary> getBeneficiary() { return beneficiary; }
    public void setBeneficiary(List<PayoutBeneficiary> beneficiary) { this.beneficiary = beneficiary; }
}
