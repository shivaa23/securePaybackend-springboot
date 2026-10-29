package com.securepay.modules.account.entity;

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
 * {@code account_statements} table — ledger entries for an {@link Account}. Columns inferred from
 * AccountStatementController: account_id, remarks, bank_id, claimed_by, credit, debit, balance,
 * particulars, bank_txnid.
 */
@Entity
@Table(name = "account_statements")
public class AccountStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "claimed_by")
    private Long claimedBy;

    @Column(name = "credit", precision = 20, scale = 2)
    private BigDecimal credit;

    @Column(name = "debit", precision = 20, scale = 2)
    private BigDecimal debit;

    @Column(name = "balance", precision = 20, scale = 2)
    private BigDecimal balance;

    @Column(name = "particulars")
    private String particulars;

    @Column(name = "bank_txnid")
    private Long bankTxnid;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }

    public Long getClaimedBy() { return claimedBy; }
    public void setClaimedBy(Long claimedBy) { this.claimedBy = claimedBy; }

    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }

    public BigDecimal getDebit() { return debit; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getParticulars() { return particulars; }
    public void setParticulars(String particulars) { this.particulars = particulars; }

    public Long getBankTxnid() { return bankTxnid; }
    public void setBankTxnid(Long bankTxnid) { this.bankTxnid = bankTxnid; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
