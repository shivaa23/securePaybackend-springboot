package com.securepay.modules.collection.entity;

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

/** {@code collections2} — dynamic-QR collection records (PayoutService::createCollection). */
@Entity
@Table(name = "collections2")
public class Collection2 implements LedgerTxn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id") private Long userId;
    @Column(name = "txn_id") private String txnId;
    @Column(name = "client_ref") private String clientRef;
    @Column(name = "operator_id") private String operatorId;
    @Column(name = "route") private String route;
    @Column(name = "pf") private String pf;
    @Column(name = "customer_mobile") private String customerMobile;
    @Column(name = "operator") private String operator;
    @Column(name = "amount", precision = 20, scale = 2) private BigDecimal amount;
    @Column(name = "charges", precision = 20, scale = 4) private BigDecimal charges;
    @Column(name = "gst", precision = 20, scale = 4) private BigDecimal gst;
    @Column(name = "ret_comm", precision = 20, scale = 4) private BigDecimal retComm;
    @Column(name = "di_comm", precision = 20, scale = 4) private BigDecimal diComm;
    @Column(name = "md_comm", precision = 20, scale = 4) private BigDecimal mdComm;
    @Column(name = "a_comm", precision = 20, scale = 4) private BigDecimal aComm;
    @Column(name = "ret_tds", precision = 20, scale = 4) private BigDecimal retTds;
    @Column(name = "di_tds", precision = 20, scale = 4) private BigDecimal diTds;
    @Column(name = "md_tds", precision = 20, scale = 4) private BigDecimal mdTds;
    @Column(name = "qr_string", columnDefinition = "text") private String qrString;
    @Column(name = "status") private String status;
    @Column(name = "api_response", columnDefinition = "longtext") private String apiResponse;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;

    @Override public String getTxnId() { return txnId; }
    @Override public String getClientRef() { return clientRef; }
    @Override public String getOperator() { return operator; }
    @Override public BigDecimal getCharges() { return charges == null ? BigDecimal.ZERO : charges; }
    @Override public BigDecimal getRetComm() { return retComm == null ? BigDecimal.ZERO : retComm; }
    @Override public BigDecimal getDiComm() { return diComm == null ? BigDecimal.ZERO : diComm; }
    @Override public BigDecimal getMdComm() { return mdComm == null ? BigDecimal.ZERO : mdComm; }
    @Override public BigDecimal getRetTds() { return retTds == null ? BigDecimal.ZERO : retTds; }
    @Override public BigDecimal getDiTds() { return diTds == null ? BigDecimal.ZERO : diTds; }
    @Override public BigDecimal getMdTds() { return mdTds == null ? BigDecimal.ZERO : mdTds; }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public void setTxnId(String v) { this.txnId = v; }
    public void setClientRef(String v) { this.clientRef = v; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String v) { this.operatorId = v; }
    public void setRoute(String v) { this.route = v; }
    public void setPf(String v) { this.pf = v; }
    public void setCustomerMobile(String v) { this.customerMobile = v; }
    public void setOperator(String v) { this.operator = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public void setCharges(BigDecimal v) { this.charges = v; }
    public void setGst(BigDecimal v) { this.gst = v; }
    public void setRetComm(BigDecimal v) { this.retComm = v; }
    public void setDiComm(BigDecimal v) { this.diComm = v; }
    public void setMdComm(BigDecimal v) { this.mdComm = v; }
    public void setAComm(BigDecimal v) { this.aComm = v; }
    public void setRetTds(BigDecimal v) { this.retTds = v; }
    public void setDiTds(BigDecimal v) { this.diTds = v; }
    public void setMdTds(BigDecimal v) { this.mdTds = v; }
    public String getQrString() { return qrString; }
    public void setQrString(String v) { this.qrString = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getApiResponse() { return apiResponse; }
    public void setApiResponse(String v) { this.apiResponse = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
