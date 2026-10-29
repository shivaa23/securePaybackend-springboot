package com.securepay.modules.dmt.entity;

import com.securepay.modules.wallet.service.LedgerTxn;
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
 * {@code dmt_txns} table (doc 06 §6.3). Implements {@link LedgerTxn} so the wallet services can
 * read its charge/commission/TDS fields.
 */
@Entity
@Table(name = "dmt_txns")
public class DmtTxn implements LedgerTxn {

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

    @Column(name = "operator")
    private String operator;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "amount", precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "ccf", precision = 20, scale = 2)
    private BigDecimal ccf;

    @Column(name = "gst", precision = 20, scale = 4)
    private BigDecimal gst;

    @Column(name = "comm", precision = 20, scale = 4)
    private BigDecimal comm;

    @Column(name = "charges", precision = 20, scale = 2)
    private BigDecimal charges;

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

    @Column(name = "mop")
    private String mop;

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

    // ----- LedgerTxn -----
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

    // ----- getters/setters -----
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

    public void setOperator(String operator) {
        this.operator = operator;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public BigDecimal getComm() {
        return comm;
    }

    public void setComm(BigDecimal comm) {
        this.comm = comm;
    }

    public void setCharges(BigDecimal charges) {
        this.charges = charges;
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

    public String getMop() {
        return mop;
    }

    public void setMop(String mop) {
        this.mop = mop;
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
}
