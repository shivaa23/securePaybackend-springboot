package com.securepay.modules.commission.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * {@code comm_rules} — retailer/distributor commission rules (doc 10 §10.5).
 * plan_id, service_name, rule_type, value_type('flat'|'percent'),
 * comm_ret, comm_dd, comm_di, comm_md, min_amount, max_amount.
 */
@Entity
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Table(name = "comm_rules")
public class CommRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "rule_type")
    private String ruleType;

    @Column(name = "value_type")
    private String valueType;

    @Column(name = "comm_ret", precision = 20, scale = 4)
    private BigDecimal commRet;

    @Column(name = "comm_dd", precision = 20, scale = 4)
    private BigDecimal commDd;

    @Column(name = "comm_di", precision = 20, scale = 4)
    private BigDecimal commDi;

    @Column(name = "comm_md", precision = 20, scale = 4)
    private BigDecimal commMd;

    @Column(name = "min_amount", precision = 20, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 20, scale = 2)
    private BigDecimal maxAmount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public BigDecimal getCommRet() {
        return commRet;
    }

    public void setCommRet(BigDecimal commRet) {
        this.commRet = commRet;
    }

    public BigDecimal getCommDd() {
        return commDd;
    }

    public void setCommDd(BigDecimal commDd) {
        this.commDd = commDd;
    }

    public BigDecimal getCommDi() {
        return commDi;
    }

    public void setCommDi(BigDecimal commDi) {
        this.commDi = commDi;
    }

    public BigDecimal getCommMd() {
        return commMd;
    }

    public void setCommMd(BigDecimal commMd) {
        this.commMd = commMd;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }
}
