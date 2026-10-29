package com.securepay.integration.impsguru;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.config.SecurePayProperties;
import com.securepay.integration.common.ProviderHttpClient;
import com.securepay.integration.common.ProviderRequest;
import com.securepay.integration.common.ProviderSign;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ImpsGuru switch — port of {@code Integration/Models/Impsguru.php} (DMT / payout / verification / AEPS).
 *
 * <p>Signing: {@code generatedHash = sha256(concatenatedFields + GURU_HKEY)}; requests carry
 * {@code Authorization: Bearer GURU_TOKEN}. The {@code guruRequest} status mapping is preserved
 * verbatim (TXN→SUCCESS, ERR/VAL→FAILED, TUP→PENDING, else→PENDING).</p>
 */
@Component
public class ImpsguruClient {

    private static final String UPI_PAY_URL = "https://api.impsguru.com/prod/v1/payments/upiPay";
    private static final String PAYOUT_URL = "https://api.impsguru.com/prod/v1/payments/payOut";
    private static final String ACC_VERIFY_URL = "https://api.impsguru.com/prod/v1/verification/bankAccountVerification";
    private static final String AEPS_CW_URL = "https://api.impsguru.com/prod/v1/aeps/cashWithdrawl";

    /** Types that return the raw decoded response (no status mapping) — verbatim from source. */
    private static final List<String> RAW_TYPES = List.of(
            "REMITTER", "DMTTXNOTP", "GETBANKS", "ACCVERIFY", "DMTTXN",
            "REGISTERREMITTER", "KYC", "VALIDATEREMITTER", "APIBAL");

    private final ProviderHttpClient http;
    private final ProviderSign sign;
    private final ObjectMapper objectMapper;
    private final SecurePayProperties.Providers cfg;

    public ImpsguruClient(ProviderHttpClient http, ProviderSign sign, ObjectMapper objectMapper,
                          SecurePayProperties properties) {
        this.http = http;
        this.sign = sign;
        this.objectMapper = objectMapper;
        this.cfg = properties.providers();
    }

    /** Fields the payout/verification builders need, read off the txn (decoupled from DmtTxn). */
    public record PayoutData(String mobileNumber, String amount, String accountNumber,
                             String beneficiaryName, String ifscCode, String mop, String txnId,
                             String ifsc, String lat, String log) {
    }

    private String hkey() {
        return cfg.guruHkey() == null ? "" : cfg.guruHkey();
    }

    /** Port of {@code Impsguru::payout($data, $service)}. */
    public Map<String, Object> payout(PayoutData d) {
        String ben = d.beneficiaryName() == null ? "" : d.beneficiaryName().replaceAll("[^A-Za-z0-9 ]", "");
        String url;
        String payload;
        if ("UPI".equals(d.mop())) {
            String hashData = n(d.mobileNumber()) + n(d.amount()) + n(d.accountNumber()) + ben
                    + n(d.txnId()) + "21" + hkey();
            String hash = sign.sha256Hex(hashData);
            url = UPI_PAY_URL;
            payload = json(ordered()
                    .add("mobileNumber", d.mobileNumber())
                    .add("transferAmount", d.amount())
                    .add("beneficiaryAccount", d.accountNumber())
                    .add("beneficiaryName", ben)
                    .add("operatorCode", "21")
                    .add("client_id", d.txnId())
                    .add("latitude", d.lat())
                    .add("longitude", d.log())
                    .add("generatedHash", hash)
                    .add("env", "Live").map());
        } else {
            String hashData = n(d.mobileNumber()) + n(d.amount()) + n(d.accountNumber()) + ben
                    + n(d.ifscCode()) + n(d.mop()) + n(d.txnId()) + "10" + hkey();
            String hash = sign.sha256Hex(hashData);
            url = PAYOUT_URL;
            payload = json(ordered()
                    .add("mobileNumber", d.mobileNumber())
                    .add("transferAmount", d.amount())
                    .add("beneficiaryAccount", d.accountNumber())
                    .add("beneficiaryName", ben)
                    .add("transferMode", d.mop())
                    .add("ifscCode", d.ifscCode())
                    .add("operatorCode", "10")
                    .add("client_id", d.txnId())
                    .add("latitude", d.lat())
                    .add("longitude", d.log())
                    .add("generatedHash", hash)
                    .add("env", "Live").map());
        }
        return guruRequest(url, "POST", payload, "PAYOUT");
    }

    /** Port of {@code Impsguru::accVerification($data, $service)}. */
    public Map<String, Object> accVerification(PayoutData d) {
        String hashData = n(d.mobileNumber()) + n(d.accountNumber()) + n(d.ifsc()) + "12"
                + n(d.txnId()) + hkey();
        String hash = sign.sha256Hex(hashData);
        String payload = json(ordered()
                .add("mobileNumber", d.mobileNumber())
                .add("beneficiaryAccount", d.accountNumber())
                .add("beneficiaryName", d.beneficiaryName())
                .add("ifscCode", d.ifsc())
                .add("operatorCode", "12")
                .add("client_id", d.txnId())
                .add("latitude", d.lat())
                .add("longitude", d.log())
                .add("generatedHash", hash)
                .add("env", "Live").map());
        return guruRequest(ACC_VERIFY_URL, "POST", payload, "ACCVERIFY");
    }

    /**
     * Port of {@code Impsguru::guruRequest($url, $method, $payload, $type)}.
     * Bearer-token POST; RAW_TYPES return the decoded body as-is; otherwise apply the
     * statuscode → status mapping (verbatim). AEPS cashWithdrawl uses operatorId, others txnReferenceId.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> guruRequest(String url, String method, String payload, String type) {
        ProviderRequest req = ProviderRequest.of(url, "POST", payload)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + (cfg.guruToken() == null ? "" : cfg.guruToken()));
        Map<String, Object> response = http.sendForJson(req);

        if (RAW_TYPES.contains(type)) {
            return response;
        }

        boolean isAeps = AEPS_CW_URL.equals(url);
        String statuscode = str(response.get("statuscode"));
        Map<String, Object> data = response.get("data") instanceof Map ? (Map<String, Object>) response.get("data") : null;

        Map<String, Object> out = new HashMap<>();
        if (statuscode == null) {
            out.put("status", "PENDING");
            out.put("operator_id", "Response Not Recieved");
            out.put("response", response);
            return out;
        }
        switch (statuscode) {
            case "TXN" -> {
                out.put("status", "SUCCESS");
                out.put("operator_id", data == null ? response.get("data")
                        : (isAeps ? data.getOrDefault("operatorId", data)
                        : data.getOrDefault("txnReferenceId", data)));
                out.put("response", response);
            }
            case "ERR", "VAL" -> {
                out.put("status", "FAILED");
                out.put("operator_id", response.getOrDefault("message", "Error"));
                out.put("response", response);
            }
            case "TUP" -> {
                out.put("status", "PENDING");
                out.put("operator_id", "");
                out.put("response", response);
            }
            default -> {
                out.put("status", "PENDING");
                out.put("operator_id", "Invalid Response");
                out.put("response", response);
            }
        }
        return out;
    }

    private String json(Map<String, Object> m) {
        try {
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String n(String v) {
        return v == null ? "" : v;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static OrderedMap ordered() {
        return new OrderedMap();
    }

    /** Tiny ordered-map builder to keep JSON field order like the source string payloads. */
    private static final class OrderedMap {
        private final Map<String, Object> m = new java.util.LinkedHashMap<>();

        OrderedMap add(String k, Object v) {
            m.put(k, v == null ? "" : v);
            return this;
        }

        Map<String, Object> map() {
            return m;
        }
    }
}
