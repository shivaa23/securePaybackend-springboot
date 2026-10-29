package com.securepay.integration.common;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Offline stand-in for the third-party payout switches (Soliteck, Abhayam, Utkal, ImpsGuru).
 *
 * <p><b>Only active when {@code securepay.providers.simulate=true}</b> (enabled in the local
 * profile) so developers can exercise the full payout flows — debit, commission, response handling,
 * refunds — without credentials and without ever touching a live money-moving endpoint. In every
 * other profile the real HTTP call is made.</p>
 *
 * <p>Failure modes are driven by the payload: if it contains {@code FAIL} the provider answers with
 * its "failed" shape; if it contains {@code PEND} it answers "pending"; otherwise "success".</p>
 */
public final class ProviderSimulator {

    private ProviderSimulator() {
    }

    public static String respond(ObjectMapper om, String url, String payload) {
        String p = payload == null ? "" : payload;
        boolean fail = p.contains("FAIL");
        boolean pend = p.contains("PEND");
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            if (url.contains("soliteck.in")) {
                body = soliteck(om, url, p, fail, pend);
            } else if (url.contains("impsguru.com")) {
                body = guru(fail, pend);
            } else if (url.contains("abhayampay.in") || url.contains("utkaldigital.co.in")) {
                body = switchApi(om, url, p, fail, pend);
            } else {
                body.put("status", "error");
                body.put("message", "No simulation for " + url);
            }
            return om.writeValueAsString(body);
        } catch (Exception e) {
            return "{}";
        }
    }

    // ------------------------------------------------------------------ Soliteck

    private static Map<String, Object> soliteck(ObjectMapper om, String url, String p,
                                                boolean fail, boolean pend) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> req = parse(om, p);
        if (url.endsWith("/GenerateToken")) {
            out.put("responseCode", 200);
            out.put("data", "SIM-TOKEN-" + rnd(6));
            return out;
        }
        out.put("responseCode", 200);
        out.put("responseMessage", "Success");
        if (url.contains("PayoutRemitterLogIn")) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("RemitterName", (str(req.get("RemiFName")) + " " + str(req.get("RemiLName"))).trim());
            d.put("RemitterMobile", str(req.get("RemitterMobile")));
            d.put("AvailableLimit", 200000);
            out.put("responseData", om.writeValueAsString(d));
        } else if (url.contains("GetDMTBanksForAPIUser")) {
            List<Map<String, Object>> banks = new ArrayList<>();
            banks.add(bank("SBIN", "State Bank of India"));
            banks.add(bank("HDFC", "HDFC Bank"));
            banks.add(bank("ICIC", "ICICI Bank"));
            out.put("responseData", om.writeValueAsString(banks));
        } else if (url.contains("ApiUserGetMasterIFSCForBank")) {
            out.put("responseData", "SIMB0000001");
        } else if (url.contains("APIUserPayoutAddBeneToList")) {
            out.put("responseData", "SIMBENE" + rnd(5));
            out.put("responseMessage", "Beneficiary added successfully");
        } else if (url.contains("APIUserPayoutDeleteBeneFromList")) {
            out.put("responseMessage", "Beneficiary deleted successfully");
        } else if (url.contains("PayoutVerifyBene")) {
            out.put("responseMessage", "Beneficiary verified");
        } else if (url.contains("PayoutTransfer")) {
            Map<String, Object> tr = new LinkedHashMap<>();
            tr.put("TransactionNumber", "SIMTXN" + rnd(8));
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("Transfers", List.of(tr));
            out.put("responseData", om.writeValueAsString(d));
            if (fail) {
                out.put("responseCode", 400);
                out.put("responseMessage", "Simulated failure");
            } else if (pend) {
                out.put("responseCode", 201);
                out.put("responseMessage", "Transaction under process");
            }
        } else if (url.contains("CheckTxnStatus")) {
            Map<String, Object> st = new LinkedHashMap<>();
            st.put("StatusId", 1);
            out.put("responseData", om.writeValueAsString(List.of(st)));
        } else {
            out.put("responseData", "{}");
        }
        return out;
    }

    // ------------------------------------------------------------------ ImpsGuru

    private static Map<String, Object> guru(boolean fail, boolean pend) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (fail) {
            out.put("statuscode", "ERR");
            out.put("message", "Simulated failure");
        } else if (pend) {
            out.put("statuscode", "TUP");
            out.put("message", "Transaction under process");
        } else {
            out.put("statuscode", "TXN");
            out.put("message", "Success");
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("txnReferenceId", "SIMGURU" + rnd(8));
            out.put("data", d);
        }
        return out;
    }

    // ------------------------------------------------------------------ Abhayam / Utkal

    private static Map<String, Object> switchApi(ObjectMapper om, String url, String p,
                                                 boolean fail, boolean pend) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> req = parse(om, p);
        out.put("Status", "Success");
        String path = url.substring(url.indexOf("/ProcessRequest/") >= 0
                ? url.indexOf("/ProcessRequest/") : 0);
        if (path.endsWith("/Check")) {
            out.put("SenderName", "SIM SENDER");
            out.put("SenderBalance", 200000);
        } else if (path.endsWith("/Register")) {
            out.put("SenderName", str(req.get("SenderName")));
            out.put("ReferenceId", String.valueOf(100000 + ThreadLocalRandom.current().nextInt(900000)));
            out.put("Description", "OTP sent successfully");
        } else if (path.endsWith("/Verify")) {
            if ("000000".equals(str(req.get("OTP")))) {
                out.put("Status", "Failed");
                out.put("Description", "Invalid OTP");
            } else {
                out.put("Description", "Sender verified successfully");
            }
        } else if (path.contains("GetDetails")) {
            out.put("Data", new ArrayList<>());
        } else if (path.contains("GetBankDetails")) {
            List<Map<String, Object>> banks = new ArrayList<>();
            banks.add(bankEntry("SBIN", "State Bank of India"));
            banks.add(bankEntry("HDFC", "HDFC Bank"));
            banks.add(bankEntry("ICIC", "ICICI Bank"));
            out.put("Data", banks);
        } else if (path.endsWith("/Add")) {
            out.put("BenficiaryId", "SIMB" + rnd(6));
            out.put("Description", "Beneficiary added");
        } else if (path.endsWith("/Delete")) {
            out.put("Description", "Beneficiary deleted");
        } else if (path.endsWith("/verify")) { // UPI VPA verification
            if (fail) {
                out.put("Status", "Failed");
                out.put("Description", "VPA verification failed");
            } else {
                out.put("Name", "SIM VPA HOLDER");
            }
        } else if (path.endsWith("/initiate")) { // dynamic QR
            out.put("Status", "Pending");
            out.put("QRString", "upi://pay?pa=sim@bank&pn=SIM&am=" + str(req.get("Amount")));
            out.put("Amount", str(req.get("Amount")));
            out.put("TransId", "SIMQR" + rnd(8));
        } else if (path.endsWith("/transaction")) { // payout / upi payout
            if (fail) {
                out.put("Status", "Failed");
                out.put("Description", "Simulated failure");
            } else if (pend) {
                out.put("Status", "Pending");
                out.put("Description", "Transaction under process");
            } else {
                out.put("OpRefId", "SIMREF" + rnd(8));
                out.put("Description", "Transaction successful");
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ helpers

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(ObjectMapper om, String p) {
        try {
            Object o = om.readValue(p, Object.class);
            return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private static Map<String, Object> bank(String code, String name) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("BankCode", code);
        m.put("BankName", name);
        return m;
    }

    private static Map<String, Object> bankEntry(String code, String name) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("BankCode", code);
        m.put("BankName", name);
        return m;
    }

    private static String rnd(int digits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits; i++) sb.append(ThreadLocalRandom.current().nextInt(10));
        return sb.toString();
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
