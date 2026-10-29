package com.securepay.modules.service.entity;

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

import java.time.LocalDateTime;

/**
 * {@code services} table — the service catalogue (doc 02 module 35, doc 10).
 *
 * <p>Key columns consumed by the money logic: {@code type} (AMTCHG/CHG/AMTCCF/...),
 * {@code name}, {@code id}, {@code route}, {@code charge_type} (doc 10 §10.3).</p>
 *
 * <p>Named {@code ServiceEntity} to avoid clashing with Spring's {@code @Service} stereotype.</p>
 */
@Entity
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Table(name = "services")
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;
    /** Short unique service code (validation: unique:services). */
    @Column(name = "code", unique = true)
    private String code;
    @Column(name = "category")
    private String category;

    @Column(name = "type")
    private String type;

    @Column(name = "route")
    private String route;

    @Column(name = "charge_type")
    private String chargeType;

    @Column(name = "status")
    private String status;

    @Column(name = "is_active")
    private Integer isActive;

    /** Service enabled for API-role users (validation: services.is_active_api = 1). */
    @Column(name = "is_active_api")
    private Integer isActiveApi;

    /** Service enabled for non-API users (validation: services.is_active_users = 1). */
    @Column(name = "is_active_users")
    private Integer isActiveUsers;

    public Integer getIsActiveApi() { return isActiveApi; }
    public void setIsActiveApi(Integer isActiveApi) { this.isActiveApi = isActiveApi; }

    public Integer getIsActiveUsers() { return isActiveUsers; }
    public void setIsActiveUsers(Integer isActiveUsers) { this.isActiveUsers = isActiveUsers; }

    // Provider-selecting operator codes used by the recharge aggregator (doc, Recharge.php).
    @Column(name = "guru")
    private String guru;

    @Column(name = "wellborn")
    private String wellborn;

    @Column(name = "shakti")
    private String shakti;

    @Column(name = "mrobotics")
    private String mrobotics;

    @Column(name = "a_one")
    private String aOne;

    @Column(name = "indicore")
    private String indicore;

    @Column(name = "sub_type")
    private String subType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getChargeType() {
        return chargeType;
    }

    public void setChargeType(String chargeType) {
        this.chargeType = chargeType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public String getGuru() {
        return guru;
    }

    public void setGuru(String guru) {
        this.guru = guru;
    }

    public String getWellborn() {
        return wellborn;
    }

    public void setWellborn(String wellborn) {
        this.wellborn = wellborn;
    }

    public String getShakti() {
        return shakti;
    }

    public void setShakti(String shakti) {
        this.shakti = shakti;
    }

    public String getMrobotics() {
        return mrobotics;
    }

    public void setMrobotics(String mrobotics) {
        this.mrobotics = mrobotics;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("a_one")

    public String getAOne() {
        return aOne;
    }

    public void setAOne(String aOne) {
        this.aOne = aOne;
    }

    public String getIndicore() {
        return indicore;
    }

    public void setIndicore(String indicore) {
        this.indicore = indicore;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
