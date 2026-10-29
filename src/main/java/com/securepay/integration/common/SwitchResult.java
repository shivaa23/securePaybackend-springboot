package com.securepay.integration.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The standard {@code {status, operator_id, response}} result shape returned by the recharge/payout
 * switch methods (doc 10 §10.8). status ∈ {SUCCESS, FAILED, PENDING}.
 */
public class SwitchResult {

    private String status = "PENDING";
    private String operatorId = "";
    private Object response;

    public static SwitchResult pending(Object response) {
        SwitchResult r = new SwitchResult();
        r.response = response;
        return r;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status);
        m.put("operator_id", operatorId);
        m.put("response", response);
        return m;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }
}
