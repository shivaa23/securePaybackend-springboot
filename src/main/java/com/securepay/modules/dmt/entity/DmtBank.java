package com.securepay.modules.dmt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** {@code dmtbanks} table — bank master used by DMT / payout beneficiary forms (per provider pipe). */
@Entity
@Table(name = "dmtbanks")
public class DmtBank {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name") private String name;
    @Column(name = "bank_id") private String bankId;
    @Column(name = "ifsc") private String ifsc;
    @Column(name = "pipe") private String pipe;
    @Column(name = "status") private Integer status;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getBankId() { return bankId; }
    public void setBankId(String v) { this.bankId = v; }
    public String getIfsc() { return ifsc; }
    public void setIfsc(String v) { this.ifsc = v; }
    public String getPipe() { return pipe; }
    public void setPipe(String v) { this.pipe = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
}
