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
 * {@code accounts} table — user settlement/credit accounts (port of Account module).
 * Columns inferred from AccountController usage: user_id, name, establishment, mobile, asm,
 * credit_limit, balance, status, type.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "name")
    private String name;

    @Column(name = "establishment")
    private String establishment;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "asm")
    private Long asm;

    @Column(name = "type")
    private String type;

    @Column(name = "credit_limit", precision = 20, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "balance", precision = 20, scale = 2)
    private BigDecimal balance;

    @Column(name = "status")
    private Integer status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEstablishment() { return establishment; }
    public void setEstablishment(String establishment) { this.establishment = establishment; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public Long getAsm() { return asm; }
    public void setAsm(Long asm) { this.asm = asm; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
