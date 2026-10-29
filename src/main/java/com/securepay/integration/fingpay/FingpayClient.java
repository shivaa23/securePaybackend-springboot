package com.securepay.integration.fingpay;

import com.securepay.config.SecurePayProperties;
import com.securepay.integration.common.ProviderHttpClient;
import com.securepay.integration.common.ProviderRequest;
import com.securepay.integration.common.ProviderSign;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * FingPay AEPS switch — port of {@code Integration/Models/Fingpay.php}.
 *
 * <p>Request security (matches the source):</p>
 * <ol>
 *   <li>Generate a 16-byte AES key (the source builds bytes 0..15), IV {@code 06f2f04cc530364f}.</li>
 *   <li>AES-128-CBC encrypt the JSON body → base64 ({@code request} body).</li>
 *   <li>RSA-encrypt the AES key with the FingPay public certificate → base64 ({@code eskey} header).</li>
 *   <li>{@code hash} header = base64(sha256(body, raw)); {@code deviceIMEI}, {@code trnTimestamp} headers.</li>
 * </ol>
 */
@Component
public class FingpayClient {

    private static final byte[] IV = "06f2f04cc530364f".getBytes(StandardCharsets.UTF_8);
    // The source builds the key as chr(0)..chr(15).
    private static final byte[] AES_KEY = buildKey();

    private final ProviderHttpClient http;
    private final ProviderSign sign;
    private final SecurePayProperties.Providers cfg;

    public FingpayClient(ProviderHttpClient http, ProviderSign sign, SecurePayProperties properties) {
        this.http = http;
        this.sign = sign;
        this.cfg = properties.providers();
    }

    /** Port of {@code Fingpay::fingPay($data)}. {@code postJson} is the request JSON string. */
    public Map<String, Object> fingPay(String url, String method, String postJson, String deviceId) {
        String device = (deviceId == null || deviceId.isBlank()) ? "14234534634565375" : deviceId;

        // 1) AES-128-CBC encrypt body → base64.
        String request = sign.aes128CbcRawBase64(postJson, AES_KEY, IV);

        // 2) RSA-encrypt the AES key with the public cert → base64 (eskey).
        String eskey = rsaEncryptKey(AES_KEY);

        // 3) hash header = base64(sha256(body, raw)).
        String hash = sign.sha256RawBase64(postJson);

        ProviderRequest req = ProviderRequest.of(url, method == null ? "POST" : method, request)
                .header("Content-Type", "text/xml")
                .header("trnTimestamp", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()))
                .header("hash", hash)
                .header("deviceIMEI", device)
                .header("eskey", eskey);
        return http.sendForJson(req);
    }

    /** Port of {@code Fingpay::threeWayReconFp($data)} — recon-key-signed hash header. */
    public Map<String, Object> threeWayReconFp(String url, String postJson, String txnDate) {
        String string = postJson + nz(cfg.fingpayReconKey());
        String hash = sign.sha256RawBase64(string);
        ProviderRequest req = ProviderRequest.of(url, "POST", postJson)
                .header("txnDate", nz(txnDate))
                .header("superMerchantLoginId", "saimoneyd")
                .header("superMerchantid", "1412")
                .header("hash", hash)
                .header("Content-Type", "text/plain");
        return http.sendForJson(req);
    }

    private String rsaEncryptKey(byte[] aesKey) {
        try (InputStream in = new ClassPathResource("certs/" + certName()).getInputStream()) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
            PublicKey publicKey = cert.getPublicKey();
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(aesKey));
        } catch (Exception e) {
            throw new IllegalStateException("FingPay RSA key-encrypt failed", e);
        }
    }

    private String certName() {
        String configured = cfg.fingpayPublicCert();
        if (configured == null || configured.isBlank()) {
            return "fingpay_public_production.cer";
        }
        // Allow either a bare filename or a path; use the filename under certs/.
        int slash = Math.max(configured.lastIndexOf('/'), configured.lastIndexOf('\\'));
        return slash >= 0 ? configured.substring(slash + 1) : configured;
    }

    private static byte[] buildKey() {
        byte[] k = new byte[16];
        for (int i = 0; i < 16; i++) {
            k[i] = (byte) i;
        }
        return k;
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }
}
