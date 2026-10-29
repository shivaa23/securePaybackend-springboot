package com.securepay.modules.payout.service;

import com.securepay.common.util.Json;
import com.securepay.integration.abhayam.AbhayamClient;
import com.securepay.integration.soliteck.SoliteckClient;
import com.securepay.integration.utkal.UtkalClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Provider access for the payout module: URL constants (verbatim from the PHP controllers), the
 * Abhayam/Utkal credential block (from env, never hardcoded) and thin wrappers over the clients.
 */
@Component
public class PayoutProviders {

    // Abhayam hosts
    public static final String AB_SENDER = "https://payoutsender.abhayampay.in/ProcessRequest/";
    public static final String AB_BENE = "https://payoutbeneficiary.abhayampay.in/ProcessRequest/";
    public static final String AB_TXN = "https://payoutransaction.abhayampay.in/ProcessRequest/transaction";
    public static final String AB_SINGLE_TXN = "https://singleptxn.abhayampay.in/ProcessRequest/transaction";
    public static final String AB_UPI_TXN = "https://upitxn.abhayampay.in/ProcessRequest/transaction";
    // Utkal hosts
    public static final String UT_SENDER = "https://payoutsender.utkaldigital.co.in/ProcessRequest/";
    public static final String UT_BENE = "https://payoutbeneficiary.utkaldigital.co.in/ProcessRequest/";
    public static final String UT_TXN = "https://payoutransaction.utkaldigital.co.in/ProcessRequest/transaction";
    public static final String UT_SINGLE_TXN = "https://singleptxn.utkaldigital.co.in/ProcessRequest/transaction";
    public static final String UT_UPI = "https://upipayout.utkaldigital.co.in/ProcessRequest/";
    public static final String UT_QR = "https://qrcollection.utkaldigital.co.in/ProcessRequest/initiate";
    // Soliteck
    public static final String SOL = "https://api-uat.soliteck.in/api/";

    private final AbhayamClient abhayamClient;
    private final UtkalClient utkalClient;
    private final SoliteckClient soliteckClient;
    private final String abhayamKey;
    private final String abhayamMpin;
    private final String utkalKey;
    private final String utkalMpin;

    public PayoutProviders(AbhayamClient abhayamClient, UtkalClient utkalClient, SoliteckClient soliteckClient,
                           @Value("${securepay.providers.abhayam-pay-key:}") String abhayamKey,
                           @Value("${securepay.providers.abhayam-pay-mpin:}") String abhayamMpin,
                           @Value("${securepay.providers.utkal-pay-key:}") String utkalKey,
                           @Value("${securepay.providers.utkal-pay-mpin:}") String utkalMpin) {
        this.abhayamClient = abhayamClient;
        this.utkalClient = utkalClient;
        this.soliteckClient = soliteckClient;
        this.abhayamKey = abhayamKey;
        this.abhayamMpin = abhayamMpin;
        this.utkalKey = utkalKey;
        this.utkalMpin = utkalMpin;
    }

    /** PHP {@code uniqid($prefix)}: prefix + 13 hex chars. */
    public static String uniqid(String prefix) {
        long micros = System.currentTimeMillis() * 1000L + ThreadLocalRandom.current().nextInt(1000);
        String hex = String.format("%08x%05x", micros / 1_000_000L, micros % 1_000_000L);
        return prefix + hex;
    }

    /** Ordered payload that starts with {@code Authcode} + {@code Mpin} for the given pipe. */
    public Map<String, Object> auth(boolean utkal) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("Authcode", utkal ? utkalKey : abhayamKey);
        m.put("Mpin", utkal ? utkalMpin : abhayamMpin);
        return m;
    }

    public Map<String, Object> abhayam(String url, Map<String, Object> payload) {
        return abhayamClient.service(url, "POST", Json.write(payload));
    }

    public Map<String, Object> utkal(String url, Map<String, Object> payload) {
        return utkalClient.service(url, "POST", Json.write(payload));
    }

    /** Soliteck takes either a map payload or a bare JSON string. */
    public Map<String, Object> soliteck(String url, Object payload) {
        return soliteckClient.service(url, "POST", Json.write(payload));
    }

    public String soliteckToken() {
        return soliteckClient.generateToken();
    }
}
