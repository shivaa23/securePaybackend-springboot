package com.securepay.modules.fundrequest.entity;

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
 * {@code fund_requests} table entity (port of App\Modules\FundRequest\Models\FundRequest).
 * Implements {@link LedgerTxn} so the wallet service can credit the user's W1 on approval; the
 * ledger txn id for fund requests is the {@code client_ref} (see WalletService.creditFundRequest).
 */
@Entity
@Table(name = "fund_requests")
public class FundRequest implements LedgerTxn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "asm_id")
    private Long asmId;

    @Column(name = "username")
    private String username;

    @Column(name = "role")
    private String role;

    @Column(name = "name")
    private String name;

    @Column(name = "amount", precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "status")
    private String status;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "mode")
    private String mode;

    @Column(name = "bank_ref_id")
    private String bankRefId;

    @Column(name = "client_ref", unique = true)
    private String clientRef;

    @Column(name = "date")
    private String date;

    @Column(name = "remark")
    private String remark;

    @Column(name = "admin_remark")
    private String adminRemark;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "receipt")
    private String receipt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // LedgerTxn: for fund requests the "txn id" used in the ledger is the client_ref.
    @Override
    public String getTxnId() {
        return clientRef;
    }

    @Override
    public String getClientRef() {
        return clientRef;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getAsmId() { return asmId; }
    public void setAsmId(Long asmId) { this.asmId = asmId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getBankRefId() { return bankRefId; }
    public void setBankRefId(String bankRefId) { this.bankRefId = bankRefId; }

    public void setClientRef(String clientRef) { this.clientRef = clientRef; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getAdminRemark() { return adminRemark; }
    public void setAdminRemark(String adminRemark) { this.adminRemark = adminRemark; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getReceipt() { return receipt; }
    public void setReceipt(String receipt) { this.receipt = receipt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
