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
 * {@code verification_txns} — account / UPI (VPA) verification charges (DmtService::createVerification).
 * Implements {@link LedgerTxn} so the wallet service can debit/refund against it.
 */
@Entity
@Table(name = "verification_txns")
public class VerificationTxn implements LedgerTxn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id") private Long userId;
    @Column(name = "txn_id") private String txnId;
    @Column(name = "client_ref") private String clientRef;
    @Column(name = "operator_id") private String operatorId;
    @Column(name = "route") private String route;
    @Column(name = "pf") private String pf;
    @Column(name = "sender_mobile") private String senderMobile;
    @Column(name = "beneficiary_name") private String beneficiaryName;
    @Column(name = "account_number") private String accountNumber;
    @Column(name = "ifsc_code") private String ifscCode;
    @Column(name = "bank_name") private String bankName;
    @Column(name = "mop") private String mop;
    @Column(name = "operator") private String operator;
    @Column(name = "amount", precision = 20, scale = 2) private BigDecimal amount;
    @Column(name = "charges", precision = 20, scale = 4) private BigDecimal charges;
    @Column(name = "gst", precision = 20, scale = 4) private BigDecimal gst;
    @Column(name = "ret_comm", precision = 20, scale = 4) private BigDecimal retComm;
    @Column(name = "di_comm", precision = 20, scale = 4) private BigDecimal diComm;
    @Column(name = "md_comm", precision = 20, scale = 4) private BigDecimal mdComm;
    @Column(name = "a_comm", precision = 20, scale = 4) private BigDecimal aComm;
    @Column(name = "location", columnDefinition = "json") private String location;
    @Column(name = "status") private String status;
    @Column(name = "api_response", columnDefinition = "longtext") private String apiResponse;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;

    @Override public String getTxnId() { return txnId; }
    @Override public String getClientRef() { return clientRef; }
    @Override public String getOperator() { return operator; }
    @Override public BigDecimal getCharges() { return charges == null ? BigDecimal.ZERO : charges; }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public void setTxnId(String v) { this.txnId = v; }
    public void setClientRef(String v) { this.clientRef = v; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String v) { this.operatorId = v; }
    public String getRoute() { return route; }
    public void setRoute(String v) { this.route = v; }
    public void setPf(String v) { this.pf = v; }
    public String getSenderMobile() { return senderMobile; }
    public void setSenderMobile(String v) { this.senderMobile = v; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String v) { this.beneficiaryName = v; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String v) { this.accountNumber = v; }
    public void setIfscCode(String v) { this.ifscCode = v; }
    public void setBankName(String v) { this.bankName = v; }
    public void setMop(String v) { this.mop = v; }
    public void setOperator(String v) { this.operator = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public void setCharges(BigDecimal v) { this.charges = v; }
    public void setGst(BigDecimal v) { this.gst = v; }
    public void setRetComm(BigDecimal v) { this.retComm = v; }
    public void setDiComm(BigDecimal v) { this.diComm = v; }
    public void setMdComm(BigDecimal v) { this.mdComm = v; }
    public void setAComm(BigDecimal v) { this.aComm = v; }
    public void setLocation(String v) { this.location = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getApiResponse() { return apiResponse; }
    public void setApiResponse(String v) { this.apiResponse = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
