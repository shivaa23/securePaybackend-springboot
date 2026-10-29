package com.securepay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Strongly-typed binding of all externalized SecurePay config under the {@code securepay.*} prefix.
 *
 * <p>Secrets are sourced from environment variables (doc 07 §7.6); never hardcode in prod.</p>
 */
@ConfigurationProperties(prefix = "securepay")
public record SecurePayProperties(
        Crypto crypto,
        Auth auth,
        App app,
        Providers providers
) {

    /** Request/response + wallet crypto keys. */
    public record Crypto(
            /** Legacy APP_KEY ("base64:..." form) — wallet column Crypt (AES-256-CBC). */
            String appKey,
            /** Request/response AES-128-CBC key + HMAC-SHA256 key. */
            String userEncryptdataKey,
            /** Request/response AES-128-CBC IV. */
            String userEncryptdataIv
    ) {
    }

    /** Password / OTP / MPIN peppers + login allow-list (doc 07 §7.1). */
    public record Auth(
            String passKey,
            String otpKey,
            String pinKey,
            List<String> allowedUserAgents,
            /** HMAC secret for signing stateless JWT login tokens (not stored in the DB). */
            String jwtSecret,
            /** JWT lifetime in minutes. */
            Long jwtTtlMinutes
    ) {
    }

    /** Response envelope constants (doc 08). */
    public record App(
            String environmentLabel,
            String timezone
    ) {
    }

    /** Provider credentials / HMAC keys (doc 07 §7.6). Sourced from env; never hardcode. */
    public record Providers(
            // ImpsGuru
            String guruHkey,
            String guruToken,
            // Paysprint
            String keyProd,
            String psPartnerId,
            // Ipay / InstantPay
            String ipayClientId,
            String ipaySecret,
            // Soliteck
            String soliteckClientId,
            String soliteckClientPass,
            String soliteckClientTpin,
            String soliteckClientSecret,
            // Enkash
            String enkashUsername,
            String enkashPassword,
            String enkashClientId,
            String enkashBasicAuth,
            // DigiKhata (Paypoint QR)
            String digikhataMerchantId,
            String digikhataAuthKey,
            String digikhataInterfaceKey,
            String digikhataIdentificationCode,
            String pay2allToken,
            // FingPay
            String fingpayClientId,
            String fingpayClientSecret,
            String fingpayReconKey,
            String fingpayPublicCert
    ) {
    }
}
