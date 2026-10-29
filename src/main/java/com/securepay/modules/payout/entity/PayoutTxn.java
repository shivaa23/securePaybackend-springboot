package com.securepay.modules.payout.entity;

import com.securepay.modules.wallet.service.LedgerTxn;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code payout_txns} table (doc 06 §6.3). Implements {@link LedgerTxn} for the wallet services.
 */
@Entity
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Table(name = "payout_txns")
public class PayoutTxn implements LedgerTxn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "txn_id")
    private String txnId;

    @Column(name = "client_ref")
    private String clientRef;

    @Column(name = "operator_id")
    private String operatorId;

    @Column(name = "route")
    private String route;

    @Column(name = "pf")
    private String pf;

    @Column(name = "sender_mobile")
    private String senderMobile;

    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "vpa")
    private String vpa;

    @Column(name = "operator")
    private String operator;

    @Column(name = "mop")
    private String mop;

    @Column(name = "amount", precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "charges", precision = 20, scale = 2)
    private BigDecimal charges;

    @Column(name = "ccf", precision = 20, scale = 2)
    private BigDecimal ccf;

    @Column(name = "gst", precision = 20, scale = 4)
    private BigDecimal gst;

    @Column(name = "ret_comm", precision = 20, scale = 4)
    private BigDecimal retComm;

    @Column(name = "di_comm", precision = 20, scale = 4)
    private BigDecimal diComm;

    @Column(name = "md_comm", precision = 20, scale = 4)
    private BigDecimal mdComm;

    @Column(name = "a_comm", precision = 20, scale = 4)
    private BigDecimal aComm;

    @Column(name = "ret_tds", precision = 20, scale = 4)
    private BigDecimal retTds;

    @Column(name = "di_tds", precision = 20, scale = 4)
    private BigDecimal diTds;

    @Column(name = "md_tds", precision = 20, scale = 4)
    private BigDecimal mdTds;

    @Column(name = "location", columnDefinition = "json")
    private String location;

    @Column(name = "status")
    private String status;

    @Column(name = "api_response", columnDefinition = "longtext")
    private String apiResponse;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public String getTxnId() {
        return txnId;
    }

    @Override
    public String getClientRef() {
        return clientRef;
    }

    @Override
    public String getOperator() {
        return operator;
    }

    @Override
    public BigDecimal getCharges() {
        return charges;
    }

    @Override
    public BigDecimal getCcf() {
        return ccf;
    }

    @Override
    public BigDecimal getRetComm() {
        return retComm;
    }

    @Override
    public BigDecimal getDiComm() {
        return diComm;
    }

    @Override
    public BigDecimal getMdComm() {
        return mdComm;
    }

    @Override
    public BigDecimal getRetTds() {
        return retTds;
    }

    @Override
    public BigDecimal getDiTds() {
        return diTds;
    }

    @Override
    public BigDecimal getMdTds() {
        return mdTds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    public void setClientRef(String clientRef) {
        this.clientRef = clientRef;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getPf() {
        return pf;
    }

    public void setPf(String pf) {
        this.pf = pf;
    }

    public String getSenderMobile() {
        return senderMobile;
    }

    public void setSenderMobile(String senderMobile) {
        this.senderMobile = senderMobile;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getVpa() {
        return vpa;
    }

    public void setVpa(String vpa) {
        this.vpa = vpa;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getMop() {
        return mop;
    }

    public void setMop(String mop) {
        this.mop = mop;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setCharges(BigDecimal charges) {
        this.charges = charges;
    }

    public void setCcf(BigDecimal ccf) {
        this.ccf = ccf;
    }

    public BigDecimal getGst() {
        return gst;
    }

    public void setGst(BigDecimal gst) {
        this.gst = gst;
    }

    public void setRetComm(BigDecimal retComm) {
        this.retComm = retComm;
    }

    public void setDiComm(BigDecimal diComm) {
        this.diComm = diComm;
    }

    public void setMdComm(BigDecimal mdComm) {
        this.mdComm = mdComm;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("a_comm")

    public BigDecimal getAComm() {
        return aComm;
    }

    public void setAComm(BigDecimal aComm) {
        this.aComm = aComm;
    }

    public void setRetTds(BigDecimal retTds) {
        this.retTds = retTds;
    }

    public void setDiTds(BigDecimal diTds) {
        this.diTds = diTds;
    }

    public void setMdTds(BigDecimal mdTds) {
        this.mdTds = mdTds;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApiResponse() {
        return apiResponse;
    }

    public void setApiResponse(String apiResponse) {
        this.apiResponse = apiResponse;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ---- columns the legacy PayoutService / handlers write that were missing ----

    /** Sender (remitter) mobile as the legacy payout API stores it (PayoutService::create). */
    @Column(name = "mobile_number")
    private String mobileNumber;

    /** Payout purpose (free text, from the request). */
    @Column(name = "purpose")
    private String purpose;

    /** Provider-side transaction id (Soliteck TransactionNumber). */
    @Column(name = "api_id")
    private String apiId;

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getApiId() { return apiId; }
    public void setApiId(String apiId) { this.apiId = apiId; }
}
