package com.securepay.integration.paysprint;

import com.securepay.config.SecurePayProperties;
import com.securepay.integration.common.ProviderHttpClient;
import com.securepay.integration.common.ProviderRequest;
import com.securepay.integration.common.ProviderSign;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Paysprint switch — port of {@code Integration/Models/Psprint.php}.
 *
 * <p>Auth: a JWT (HS256) built from {@code {timestamp, partnerId, reqid}} signed with KEY_PROD,
 * sent as the {@code Token} header.</p>
 */
@Component
public class PaysprintClient {

    private final ProviderHttpClient http;
    private final ProviderSign sign;
    private final SecurePayProperties.Providers cfg;

    public PaysprintClient(ProviderHttpClient http, ProviderSign sign, SecurePayProperties properties) {
        this.http = http;
        this.sign = sign;
        this.cfg = properties.providers();
    }

    /** Port of {@code Psprint::service($data)} — {@code orderId} becomes the JWT {@code reqid}. */
    public Map<String, Object> service(String url, String method, String payload, String orderId) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("timestamp", Instant.now().getEpochSecond());
        claims.put("partnerId", cfg.psPartnerId() == null ? "PS004357" : cfg.psPartnerId());
        claims.put("reqid", orderId);

        String token = sign.jwtHs256(claims, cfg.keyProd() == null ? "" : cfg.keyProd());

        ProviderRequest req = ProviderRequest.of(url, method == null ? "POST" : method, payload)
                .header("Token", token)
                .header("Content-Type", "application/json");
        return http.sendForJson(req);
    }
}
