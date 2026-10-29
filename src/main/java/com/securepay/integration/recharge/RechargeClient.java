package com.securepay.integration.recharge;

import com.securepay.config.SecurePayProperties;
import com.securepay.integration.common.ProviderHttpClient;
import com.securepay.integration.common.ProviderRequest;
import com.securepay.integration.common.ProviderSign;
import com.securepay.integration.common.SwitchResult;
import com.securepay.integration.indicore.IndicoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recharge aggregator — port of {@code Integration/Models/Recharge.php}. One method per operator
 * route; each returns a {@link SwitchResult} with the same status-mapping as the source.
 *
 * <p>Operator-selecting codes (guru/wellborn/shakti/mrobotics/a_one/indicore) come off the service
 * row. Credentials/tokens come from env (not hardcoded).</p>
 */
@Component
public class RechargeClient {

    private final ProviderHttpClient http;
    private final ProviderSign sign;
    private final IndicoreClient indicoreClient;
    private final SecurePayProperties.Providers cfg;

    @Value("${securepay.recharge.wb-token:}")
    private String wbToken;
    @Value("${securepay.recharge.shk-token:}")
    private String shkToken;
    @Value("${securepay.recharge.mr1-token:}")
    private String mr1Token;
    @Value("${securepay.recharge.aone-token:}")
    private String aoneToken;
    @Value("${securepay.recharge.indicore-signature:}")
    private String indicoreSignature;

    public RechargeClient(ProviderHttpClient http, ProviderSign sign, IndicoreClient indicoreClient,
                          SecurePayProperties properties) {
        this.http = http;
        this.sign = sign;
        this.indicoreClient = indicoreClient;
        this.cfg = properties.providers();
    }

    /** Operator-agnostic input for a recharge. */
    public record RechargeData(String mobileNumber, String amount, String txnId, String subType) {
    }

    /** Port of {@code Recharge::guru} — ImpsGuru mobile/DTH recharge, SHA-256 hash + Bearer. */
    @SuppressWarnings("unchecked")
    public SwitchResult guru(RechargeData d, String guruCode) {
        String hkey = cfg.guruHkey() == null ? "" : cfg.guruHkey();
        String hash = sign.sha256Hex(n(d.mobileNumber()) + n(d.amount()) + n(guruCode) + n(d.txnId()) + hkey);

        boolean isMobile = List.of("1", "2", "3", "4", "13").contains(guruCode);
        String url = isMobile ? "https://api.impsguru.com/prod/v1/recharge/mobile"
                : "https://api.impsguru.com/prod/v1/recharge/dth";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(isMobile ? "mobileNumber" : "customerNumber", d.mobileNumber());
        payload.put("transferAmount", d.amount());
        payload.put("operatorCode", guruCode);
        payload.put("client_id", d.txnId());
        payload.put("latitude", "29.1234");
        payload.put("longitude", "72.1234");
        payload.put("env", "Live");
        payload.put("generatedHash", hash);

        ProviderRequest req = ProviderRequest.of(url, "POST", write(payload))
                .header("Authorization", "Bearer " + (cfg.guruToken() == null ? "" : cfg.guruToken()))
                .header("Content-Type", "application/json");
        Map<String, Object> response = http.sendForJson(req);

        SwitchResult result = SwitchResult.pending(response.isEmpty() ? "No Response Received" : response);
        String statuscode = str(response.get("statuscode"));
        if (statuscode != null) {
            if ("TXN".equals(statuscode)) {
                result.setStatus("SUCCESS");
                Object data = response.get("data");
                if (data instanceof Map<?, ?> m) {
                    result.setOperatorId(str(((Map<String, Object>) m).get("txnReferenceId")));
                }
            } else if ("ERR".equals(statuscode) || "VAL".equals(statuscode)) {
                result.setStatus("FAILED");
            } else if ("TUP".equals(statuscode)) {
                result.setStatus("PENDING");
            }
            result.setResponse(response);
        }
        return result;
    }

    /** Port of {@code Recharge::wb} — Wellborn, comma-separated response body. */
    public SwitchResult wb(RechargeData d, String oprCode) {
        String url = "http://wellbornapi.in/webservices/httpapi/recharge-request-new?"
                + "api_key=" + enc(wbToken) + "&acc_no=123886&rech_num=" + enc(d.mobileNumber())
                + "&amount=" + enc(d.amount()) + "&client_key=" + enc(d.txnId()) + "&opr_code=" + enc(oprCode);
        String body = http.send(ProviderRequest.of(url, "GET", null)).body();
        SwitchResult result = SwitchResult.pending(body == null ? "No Response Received" : body);
        if (body != null && !body.isBlank()) {
            String[] res = body.split(",");
            if (res.length > 0) {
                switch (res[0]) {
                    case "success", "received" -> result.setStatus("SUCCESS");
                    case "failure" -> result.setStatus("FAILED");
                    default -> result.setStatus("PENDING");
                }
                String opid = res.length > 6 ? res[6] : "";
                if ((opid == null || opid.isEmpty()) && res.length > 7) {
                    opid = res[7];
                }
                result.setOperatorId(opid == null ? "" : opid);
                result.setResponse(List.of(res));
            }
        }
        return result;
    }

    /** Port of {@code Recharge::shakti} — ShaktiFintech, query-token GET. */
    public SwitchResult shakti(RechargeData d, String opId) {
        String url = "https://shaktifintech.in/Api/Service/Recharge2?ApiToken=" + enc(shkToken)
                + "&MobileNo=" + enc(d.mobileNumber()) + "&Amount=" + enc(d.amount())
                + "&OpId=" + enc(opId) + "&RefTxnId=" + enc(d.txnId());
        Map<String, Object> res = http.sendForJson(ProviderRequest.of(url, "GET", null));
        SwitchResult result = SwitchResult.pending(res.isEmpty() ? "No Response Received" : res);
        result.setOperatorId("NA");
        Object status = res.get("STATUS");
        if (status != null) {
            result.setOperatorId(str(res.getOrDefault("OPTXNID", "")));
            String s = status.toString();
            if ("1".equals(s)) {
                result.setStatus("SUCCESS");
            } else if ("3".equals(s)) {
                result.setStatus("FAILED");
            } else {
                result.setStatus("PENDING");
            }
        }
        return result;
    }

    /** Port of {@code Recharge::mrb} — mRobotics, form POST. */
    public SwitchResult mrb(RechargeData d, String companyId) {
        String body = "api_token=" + enc(mr1Token) + "&mobile_no=" + enc(d.mobileNumber())
                + "&amount=" + enc(d.amount()) + "&company_id=" + enc(companyId)
                + "&order_id=" + enc(d.txnId()) + "&is_stv=false";
        ProviderRequest req = ProviderRequest.of("https://mrobotics.in/api/recharge", "POST", body)
                .header("Content-Type", "application/x-www-form-urlencoded");
        Map<String, Object> res = http.sendForJson(req);
        SwitchResult result = SwitchResult.pending(res.isEmpty() ? "No Response Received" : res);
        result.setOperatorId("NA");
        Object status = res.get("status");
        if (status != null) {
            String s = status.toString();
            if ("success".equals(s)) {
                result.setStatus("SUCCESS");
            } else if ("failure".equals(s)) {
                result.setStatus("FAILED");
            } else {
                result.setStatus("PENDING");
            }
            String opid = str(res.getOrDefault("tnx_id", ""));
            result.setOperatorId(opid == null || opid.isEmpty() ? "NA" : opid);
        }
        return result;
    }

    /** Port of {@code Recharge::aone} — A1 Recharge Solutions, query-token GET. */
    public SwitchResult aone(RechargeData d, String spKey) {
        String url = "https://a1rechargesolutions.in/API/TransactionAPI?UserID=11698"
                + "&Token=" + enc(aoneToken) + "&Account=" + enc(d.mobileNumber())
                + "&Amount=" + enc(d.amount()) + "&APIRequestID=" + enc(d.txnId())
                + "&SPKey=" + enc(spKey) + "&Format=1";
        Map<String, Object> spc = http.sendForJson(ProviderRequest.of(url, "GET", null));
        SwitchResult result = SwitchResult.pending(spc.isEmpty() ? "TRANSACTION PENDING" : spc);
        result.setOperatorId("NA");
        Object status = spc.get("status");
        if (status != null) {
            String s = status.toString();
            if ("2".equals(s)) {
                result.setStatus("SUCCESS");
                result.setResponse("TRANSACTION SUCCESSFUL");
            } else if ("3".equals(s)) {
                result.setStatus("FAILED");
                result.setResponse(spc.getOrDefault("msg", "FAILED"));
            } else if ("1".equals(s)) {
                result.setStatus("PENDING");
                result.setResponse("TRANSACTION PENDING");
            }
            result.setOperatorId(str(spc.getOrDefault("opid", "NA")));
        }
        return result;
    }

    /** Port of {@code Recharge::indicore} — pipe-delimited response via the Indicore client. */
    public SwitchResult indicore(RechargeData d, String optr) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("signature", indicoreSignature);
        payload.put("rnum", d.mobileNumber());
        payload.put("ramt", d.amount());
        payload.put("optr", optr);
        payload.put("type", "dth".equals(d.subType()) ? "DTH" : "REC");
        payload.put("cack", d.txnId());

        String resp = indicoreClient.service("https://eabi.indicore.net/live", "POST", payload);
        if (resp == null || resp.isBlank()) {
            SwitchResult r = SwitchResult.pending("No response from Indicore API");
            return r;
        }
        String[] parts = resp.trim().split("\\|");
        String code = parts.length > 0 ? parts[0] : null;
        String msg = parts.length > 1 ? parts[1].trim().toUpperCase() : "";
        String operatorRef = parts.length > 3 ? parts[3] : "0";

        SwitchResult result = SwitchResult.pending(resp);
        result.setStatus("FAILED");
        if ("0".equals(code) || "SUCCESS".equals(msg)) {
            result.setStatus("SUCCESS");
        } else if ("PENDING".equals(msg) || "PROCESS".equals(msg)) {
            result.setStatus("PENDING");
        }
        result.setOperatorId(operatorRef);
        return result;
    }

    private String write(Map<String, Object> m) {
        try {
            return http.objectMapper().writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    private static String n(String v) {
        return v == null ? "" : v;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
