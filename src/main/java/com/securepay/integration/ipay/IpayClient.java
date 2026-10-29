package com.securepay.integration.ipay;

import com.securepay.config.SecurePayProperties;
import com.securepay.integration.common.ProviderHttpClient;
import com.securepay.integration.common.ProviderRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * iPay / InstantPay switch — port of {@code Integration/Models/Ipay.php} (AEPS, DMR, payout, BBPS, CC).
 *
 * <p>Auth: client-id/secret headers {@code X-Ipay-Client-Id} / {@code X-Ipay-Client-Secret};
 * AEPS flows carry an {@code hmac} inside the JSON payload (built by the caller). This client sends
 * the prepared payload to the given InstantPay endpoint.</p>
 */
@Component
public class IpayClient {

    private final ProviderHttpClient http;
    private final SecurePayProperties.Providers cfg;

    public IpayClient(ProviderHttpClient http, SecurePayProperties properties) {
        this.http = http;
        this.cfg = properties.providers();
    }

    /** Generic port of {@code Ipay::service($data)} and the per-endpoint methods (same header auth). */
    public Map<String, Object> service(String url, String method, String payload) {
        ProviderRequest req = ProviderRequest.of(url, method == null ? "POST" : method, payload)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-Ipay-Client-Id", nz(cfg.ipayClientId()))
                .header("X-Ipay-Client-Secret", nz(cfg.ipaySecret()));
        return http.sendForJson(req);
    }

    /** AEPS cash withdrawal endpoint (port of {@code Ipay::cashWithdrawl}). */
    public Map<String, Object> cashWithdrawl(String payload) {
        return service("https://api.instantpay.in/fi/aeps/cashWithdrawal", "POST", payload);
    }

    /** AEPS balance inquiry (port of {@code Ipay::balanceInquiry}). */
    public Map<String, Object> balanceInquiry(String payload) {
        return service("https://api.instantpay.in/fi/aeps/balanceInquiry", "POST", payload);
    }

    /** AEPS mini statement (port of {@code Ipay::miniStatement}). */
    public Map<String, Object> miniStatement(String payload) {
        return service("https://api.instantpay.in/fi/aeps/miniStatement", "POST", payload);
    }

    /** AadhaarPay (port of {@code Ipay::AadharPay}). */
    public Map<String, Object> aadhaarPay(String payload) {
        return service("https://api.instantpay.in/fi/aeps/aadhaarPay", "POST", payload);
    }

    /** Payout (port of {@code Ipay::payOut}). */
    public Map<String, Object> payOut(String payload) {
        return service("https://api.instantpay.in/payments/payout", "POST", payload);
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }
}
