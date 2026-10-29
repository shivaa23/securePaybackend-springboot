package com.securepay.modules.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.securepay.common.crypto.EncryptedPaiseConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * {@code users} table entity. Port of the source {@code User} model (doc 06 §6.3, doc 10 §10.1).
 *
 * <ul>
 *   <li>Hidden fields: {@code password}, {@code api_token}, {@code mpin} (@JsonIgnore).</li>
 *   <li>Wallet columns {@code w1/w2/w3/lien} encrypted at rest, held as integer paise (long).</li>
 *   <li>{@code parent} self-references the hierarchy (downline/descendant queries).</li>
 * </ul>
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @JsonIgnore
    @Column(name = "password")
    private String password;

    @Column(name = "pan")
    private String pan;

    @JsonIgnore
    @Column(name = "api_token")
    private String apiToken;

    // Encrypted integer-paise wallets (doc 10 §10.1).
    @Convert(converter = EncryptedPaiseConverter.class)
    @Column(name = "w1")
    private Long w1;

    @Convert(converter = EncryptedPaiseConverter.class)
    @Column(name = "w2")
    private Long w2;

    @Convert(converter = EncryptedPaiseConverter.class)
    @Column(name = "w3")
    private Long w3;

    @Convert(converter = EncryptedPaiseConverter.class)
    @Column(name = "lien")
    private Long lien;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "vpa")
    private String vpa;

    @JsonIgnore
    @Column(name = "mpin")
    private String mpin;

    @Column(name = "parent")
    private Long parent;

    @Column(name = "establishment")
    private String establishment;

    @Column(name = "role")
    private String role;

    @Column(name = "is_active")
    private Integer isActive;

    @Column(name = "two_fa")
    private String twoFa;

    @Column(name = "whitelisted_ips")
    private String whitelistedIps;

    @Column(name = "imei")
    private String imei;

    @Column(name = "instId")
    private String instId;

    @Column(name = "encryption_key")
    private String encryptionKey;

    @Column(name = "plan_id")
    private Long planId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ------------------------------------------------------------------
    // Wallet increment/decrement helpers — port of User.php (doc 10 §10.1).
    // Amounts are in PAISE (callers multiply by 100 before calling).
    // NOTE the behavioural asymmetry: decrementW1 allows negative; w2/w3/lien floor at 0.
    // ------------------------------------------------------------------

    public void incrementW1(long amount) {
        this.w1 = nz(this.w1) + amount;
    }

    public void decrementW1(long amount) {
        this.w1 = nz(this.w1) - amount; // Allow negative values
    }

    public void incrementW2(long amount) {
        this.w2 = nz(this.w2) + amount;
    }

    public void decrementW2(long amount) {
        this.w2 = Math.max(0, nz(this.w2) - amount);
    }

    public void incrementW3(long amount) {
        this.w3 = nz(this.w3) + amount;
    }

    public void decrementW3(long amount) {
        this.w3 = Math.max(0, nz(this.w3) - amount);
    }

    public void incrementLien(long amount) {
        this.lien = nz(this.lien) + amount;
    }

    public void decrementLien(long amount) {
        this.lien = Math.max(0, nz(this.lien) - amount);
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    // ------------------------------------------------------------------
    // Getters / setters
    // ------------------------------------------------------------------

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public Long getW1() {
        return w1;
    }

    public void setW1(Long w1) {
        this.w1 = w1;
    }

    public Long getW2() {
        return w2;
    }

    public void setW2(Long w2) {
        this.w2 = w2;
    }

    public Long getW3() {
        return w3;
    }

    public void setW3(Long w3) {
        this.w3 = w3;
    }

    public Long getLien() {
        return lien;
    }

    public void setLien(Long lien) {
        this.lien = lien;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getVpa() {
        return vpa;
    }

    public void setVpa(String vpa) {
        this.vpa = vpa;
    }

    public String getMpin() {
        return mpin;
    }

    public void setMpin(String mpin) {
        this.mpin = mpin;
    }

    public Long getParent() {
        return parent;
    }

    public void setParent(Long parent) {
        this.parent = parent;
    }

    public String getEstablishment() {
        return establishment;
    }

    public void setEstablishment(String establishment) {
        this.establishment = establishment;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public String getTwoFa() {
        return twoFa;
    }

    public void setTwoFa(String twoFa) {
        this.twoFa = twoFa;
    }

    public String getWhitelistedIps() {
        return whitelistedIps;
    }

    public void setWhitelistedIps(String whitelistedIps) {
        this.whitelistedIps = whitelistedIps;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getInstId() {
        return instId;
    }

    public void setInstId(String instId) {
        this.instId = instId;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
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
