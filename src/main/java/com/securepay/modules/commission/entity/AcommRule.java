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
 * {@code acomm_rules} — admin commission rules (doc 10 §10.5).
 * route, service_name, rule_type, value_type, a_comm, min_amount, max_amount.
 */
@Entity
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Table(name = "acomm_rules")
public class AcommRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route")
    private String route;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "rule_type")
    private String ruleType;

    @Column(name = "value_type")
    private String valueType;

    @Column(name = "a_comm", precision = 20, scale = 4)
    private BigDecimal aComm;

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

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
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

    @com.fasterxml.jackson.annotation.JsonProperty("a_comm")

    public BigDecimal getAComm() {
        return aComm;
    }

    public void setAComm(BigDecimal aComm) {
        this.aComm = aComm;
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
