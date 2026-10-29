package com.securepay.modules.commission.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typed mirror of the PHP associative commission array {@code {ret, dd, di, md, type, rule_type}}
 * (doc 10 §10.5). Preserves {@code isset}/null semantics: a tier that was never set is absent
 * ({@code null}), and callers treat absent as 0.
 *
 * <p>Empty result (no rule configured) → an instance with all tiers null and {@link #isEmpty()} true,
 * matching the source returning {@code []}.</p>
 */
public class CommissionResult {

    private BigDecimal ret;
    private BigDecimal dd;
    private BigDecimal di;
    private BigDecimal md;
    private String type;      // 'flat' | 'percent'
    private String ruleType;
    private boolean present;  // false == source returned []

    public static CommissionResult empty() {
        return new CommissionResult();
    }

    public boolean isEmpty() {
        return !present;
    }

    public boolean isPresent() {
        return present;
    }

    public void markPresent() {
        this.present = true;
    }

    public boolean hasDi() {
        return di != null;
    }

    public boolean hasMd() {
        return md != null;
    }

    /** Builds the ordered map form (for serialization / debugging) like the PHP array. */
    public Map<String, Object> asMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (ret != null) {
            m.put("ret", ret);
        }
        if (dd != null) {
            m.put("dd", dd);
        }
        if (di != null) {
            m.put("di", di);
        }
        if (md != null) {
            m.put("md", md);
        }
        if (type != null) {
            m.put("type", type);
        }
        if (ruleType != null) {
            m.put("rule_type", ruleType);
        }
        return m;
    }

    public BigDecimal getRet() {
        return ret;
    }

    public void setRet(BigDecimal ret) {
        this.ret = ret;
    }

    public BigDecimal getDd() {
        return dd;
    }

    public void setDd(BigDecimal dd) {
        this.dd = dd;
    }

    public BigDecimal getDi() {
        return di;
    }

    public void setDi(BigDecimal di) {
        this.di = di;
    }

    public BigDecimal getMd() {
        return md;
    }

    public void setMd(BigDecimal md) {
        this.md = md;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }
}
