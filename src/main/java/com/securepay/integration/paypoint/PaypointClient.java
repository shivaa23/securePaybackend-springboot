package com.securepay.integration.paypoint;

import com.securepay.config.SecurePayProperties;
import com.securepay.integration.common.ProviderHttpClient;
import com.securepay.integration.common.ProviderRequest;
import com.securepay.integration.common.ProviderResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Paypoint / DigiKhata provider — port of {@code Integration/Models/Paypoint.php}.
 *
 * <ul>
 *   <li>{@code service} — Bearer-token JSON call (token supplied by caller).</li>
 *   <li>{@code serviceQr} — DigiKhata QR call with MerchantID/AuthKey/InterfaceKey headers.</li>
 *   <li>{@code apiBal} — Pay2All balance (GET with Bearer token).</li>
 * </ul>
 */
@Component
public class PaypointClient {

    private static final String PAY2ALL_USER_URL = "https://erp.pay2all.in/api/user";

    private final ProviderHttpClient http;
    private final SecurePayProperties.Providers cfg;

    public PaypointClient(ProviderHttpClient http, SecurePayProperties properties) {
        this.http = http;
        this.cfg = properties.providers();
    }

    /** Port of {@code Paypoint::service($data)} — Bearer token supplied by the caller. */
    public Map<String, Object> service(String url, String method, String payload, String token) {
        ProviderRequest req = ProviderRequest.of(url, method == null ? "POST" : method, payload)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + nz(token));
        return http.sendForJson(req);
    }

    /** Port of {@code Paypoint::serviceQr($data)} — DigiKhata headers; returns the raw response body. */
    public String serviceQr(String url, String method, String payload) {
        ProviderRequest req = ProviderRequest.of(url, method == null ? "POST" : method, payload)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("MerchantID", nz(cfg.digikhataMerchantId()))
                .header("AuthKey", nz(cfg.digikhataAuthKey()))
                .header("InterfaceKey", nz(cfg.digikhataInterfaceKey()));
        ProviderResponse res = http.send(req);
        return res.body();
    }

    /** Port of {@code Paypoint::apiBal()} — Pay2All user/balance (GET, Bearer). */
    public Map<String, Object> apiBal() {
        ProviderRequest req = ProviderRequest.of(PAY2ALL_USER_URL, "GET", null)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + nz(cfg.pay2allToken()));
        return http.sendForJson(req);
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }
}
