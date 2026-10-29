package com.securepay.modules.payout.service;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.util.Json;
import com.securepay.modules.dmt.entity.VerificationTxn;
import com.securepay.modules.dmt.repository.VerificationTxnRepository;
import com.securepay.modules.payout.entity.PayoutSender;
import com.securepay.modules.payout.entity.PayoutTxn;
import com.securepay.modules.payout.repository.PayoutSenderRepository;
import com.securepay.modules.payout.repository.PayoutTxnRepository;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import com.securepay.modules.wallet.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Faithful port of {@code Payout/Services/PayoutResponseHandler}. Every method must run inside a
 * transaction (the caller opens one) and returns the response envelope; business "errors" such as
 * FAILED are returned (not thrown) so the refund + txn update commit.
 *
 * <p>Parity quirks kept on purpose: PENDING / unknown / no-response branches still credit the
 * distributor + master-distributor commission and consume the sender's {@code rem_limit}; the
 * ImpsGuru {@link #handle} credits the MD independently of the DI amount, the other handlers only
 * when the DI commission is &gt; 0.</p>
 */
@Service
public class PayoutResponseHandler {

    private final WalletService walletService;
    private final UserRepository userRepository;
    private final PayoutTxnRepository payoutTxnRepository;
    private final PayoutSenderRepository senderRepository;
    private final VerificationTxnRepository verificationTxnRepository;
    private final ResponseFactory response;

    public PayoutResponseHandler(WalletService walletService, UserRepository userRepository,
                                 PayoutTxnRepository payoutTxnRepository,
                                 PayoutSenderRepository senderRepository,
                                 VerificationTxnRepository verificationTxnRepository,
                                 ResponseFactory response) {
        this.walletService = walletService;
        this.userRepository = userRepository;
        this.payoutTxnRepository = payoutTxnRepository;
        this.senderRepository = senderRepository;
        this.verificationTxnRepository = verificationTxnRepository;
        this.response = response;
    }

    // ------------------------------------------------------------------ helpers
    private static boolean positive(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }

    private static Object get(Map<String, Object> m, String k) {
        return m == null ? null : m.get(k);
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private void save(PayoutTxn t) {
        payoutTxnRepository.save(t);
    }

    /** DI (and MD) commission credit. {@code mdIndependent}: ImpsGuru credits MD without needing DI &gt; 0. */
    private void creditUplines(PayoutTxn txn, User user, ServiceEntity service, boolean mdIndependent) {
        if (user.getParent() == null) {
            return;
        }
        User distributor = userRepository.findById(user.getParent()).orElse(null);
        if (distributor == null) {
            return;
        }
        boolean diDue = positive(txn.getDiComm());
        if (diDue) {
            walletService.creditCommission(distributor, txn.getDiComm(), txn, service);
        }
        if ((diDue || mdIndependent) && distributor.getParent() != null && positive(txn.getMdComm())) {
            User md = userRepository.findById(distributor.getParent()).orElse(null);
            if (md != null) {
                walletService.creditCommission(md, txn.getMdComm(), txn, service);
            }
        }
    }

    private void consumeLimit(PayoutSender sender, PayoutTxn txn) {
        PayoutSender s = senderRepository.findById(sender.getId()).orElse(sender);
        BigDecimal rem = s.getRemLimit() == null ? BigDecimal.ZERO : s.getRemLimit();
        s.setRemLimit(rem.subtract(txn.getAmount()));
        senderRepository.save(s);
    }

    private ResponseEntity<Map<String, Object>> ok(String msg, Object data, int code) {
        return response.success(msg, data, HttpStatus.valueOf(code));
    }

    private ResponseEntity<Map<String, Object>> err(String msg, Object errors, int code) {
        return response.error(msg, errors, HttpStatus.valueOf(code));
    }

    // ------------------------------------------------------------------ ImpsGuru
    /** Port of {@code handle} — ImpsGuru {@code {status, operator_id, response}} shape. */
    public ResponseEntity<Map<String, Object>> handle(PayoutTxn txn, Map<String, Object> resp, User user,
                                                      ServiceEntity service, PayoutSender sender) {
        if (resp == null || resp.get("status") == null) {
            txn.setApiResponse(Json.write(Map.of("error", "No response")));
            save(txn);
            creditUplines(txn, user, service, true);
            return ok("Fund Transfer Pending", "No response", 201);
        }
        String status = str(resp.get("status"));
        switch (status) {
            case "SUCCESS" -> {
                txn.setStatus("SUCCESS");
                txn.setOperatorId(str(resp.get("operator_id")));
                txn.setApiResponse(Json.write(resp));
                save(txn);
                creditUplines(txn, user, service, true);
                consumeLimit(sender, txn);
                return ok("Fund Transfer Success", resp, 200);
            }
            case "PENDING" -> {
                txn.setOperatorId(str(resp.get("operator_id")));
                txn.setApiResponse(Json.write(resp));
                save(txn);
                creditUplines(txn, user, service, true);
                consumeLimit(sender, txn);
                return ok("Fund Transfer Success", resp, 200);
            }
            case "FAILED" -> {
                walletService.refund(user, txn.getAmount(), txn, service);
                txn.setStatus("FAILED");
                txn.setOperatorId(str(resp.get("operator_id")));
                txn.setApiResponse(Json.write(resp));
                save(txn);
                return err(str(resp.get("operator_id")), resp, 403);
            }
            default -> {
                txn.setApiResponse(Json.write(resp));
                save(txn);
                creditUplines(txn, user, service, true);
                consumeLimit(sender, txn);
                return err("Fund Transfer Unknown", resp, 403);
            }
        }
    }

    // ------------------------------------------------------------------ Soliteck
    /** Port of {@code handleResponse} — Soliteck {@code responseCode} shape. */
    public ResponseEntity<Map<String, Object>> handleResponse(PayoutTxn txn, Map<String, Object> resp, User user,
                                                              ServiceEntity service, PayoutSender sender) {
        if (resp == null || resp.get("responseCode") == null) {
            creditUplines(txn, user, service, false);
            return ok("Transaction pending", java.util.List.of(), 201);
        }
        Map<String, Object> data = Json.readMap(str(resp.get("responseData")));
        String transactionNumber = null;
        if (data != null && data.get("Transfers") instanceof java.util.List<?> list && !list.isEmpty()
                && list.get(0) instanceof Map<?, ?> first && first.get("TransactionNumber") != null) {
            transactionNumber = first.get("TransactionNumber").toString();
        }
        String code = str(resp.get("responseCode"));
        switch (code) {
            case "200" -> {
                txn.setStatus("SUCCESS");
                txn.setOperatorId("Transaction submitted successfully");
                txn.setApiId(transactionNumber);
                txn.setApiResponse(Json.write(resp));
                save(txn);
                creditUplines(txn, user, service, false);
                consumeLimit(sender, txn);
                return ok("Fund Transfer Success", txn, 200);
            }
            case "201" -> {
                txn.setStatus("PENDING");
                txn.setOperatorId("Transaction Under Process");
                txn.setApiId(transactionNumber);
                txn.setApiResponse(Json.write(resp));
                save(txn);
                creditUplines(txn, user, service, false);
                consumeLimit(sender, txn);
                return ok("Fund Transfer Under Process", txn, 200);
            }
            case "400" -> {
                walletService.refund(user, txn.getAmount(), txn, service);
                txn.setStatus("FAILED");
                txn.setOperatorId("Transaction failed");
                txn.setApiResponse(Json.write(resp));
                save(txn);
                return err("Transaction failed", resp, 400);
            }
            default -> {
                txn.setOperatorId("Transaction submitted successfully");
                txn.setApiResponse(Json.write(resp));
                save(txn);
                creditUplines(txn, user, service, false);
                consumeLimit(sender, txn);
                return err("Fund Transfer Under Process", resp, 403);
            }
        }
    }

    // ------------------------------------------------------------------ Abhayam / Utkal
    /** Port of {@code handleResponseAbhyam} — Abhayam/Utkal {@code Status} shape. */
    public ResponseEntity<Map<String, Object>> handleResponseAbhyam(PayoutTxn txn, Map<String, Object> resp,
                                                                    User user, ServiceEntity service,
                                                                    PayoutSender sender) {
        if (resp == null || resp.get("Status") == null) {
            creditUplines(txn, user, service, false);
            return ok("Transaction under process", java.util.List.of(), 201);
        }
        String status = str(resp.get("Status"));
        switch (status) {
            case "Success" -> {
                creditUplines(txn, user, service, false);
                txn.setStatus("SUCCESS");
                txn.setOperatorId(str(resp.get("OpRefId")));
                txn.setApiResponse(Json.write(resp));
                save(txn);
                consumeLimit(sender, txn);
                return ok("Fund Transfer Success", txn, 200);
            }
            case "Pending" -> {
                txn.setStatus("PENDING");
                txn.setOperatorId("Transaction Under Process");
                txn.setApiResponse(Json.write(resp));
                save(txn);
                creditUplines(txn, user, service, false);
                consumeLimit(sender, txn);
                return ok("Fund Transfer Under Process", txn, 200);
            }
            case "Failed" -> {
                walletService.refund(user, txn.getAmount(), txn, service);
                txn.setStatus("FAILED");
                txn.setOperatorId("Transaction failed");
                txn.setApiResponse(Json.write(resp));
                save(txn);
                return err("Transaction failed", resp, 400);
            }
            default -> {
                txn.setOperatorId("Transaction submitted successfully");
                txn.setApiResponse(Json.write(resp));
                save(txn);
                creditUplines(txn, user, service, false);
                consumeLimit(sender, txn);
                return err("Fund Transfer Under Process", resp, 403);
            }
        }
    }

    // ------------------------------------------------------------------ UPI (VPA) verification
    /** Port of {@code handleUpiVerification}. */
    public ResponseEntity<Map<String, Object>> handleUpiVerification(VerificationTxn txn, Map<String, Object> resp,
                                                                      User user, ServiceEntity service) {
        if (resp == null || resp.get("Status") == null) {
            txn.setApiResponse(Json.write(Map.of("error", "No response from API")));
            verificationTxnRepository.save(txn);
            return err("Fund Transfer Pending", "No response", 500);
        }
        String status = str(resp.get("Status"));
        switch (status) {
            case "Success" -> {
                txn.setStatus("SUCCESS");
                Object d = resp.get("data");
                txn.setOperatorId(d instanceof Map<?, ?> m && m.get("txnReferenceId") != null
                        ? m.get("txnReferenceId").toString() : null);
                txn.setBeneficiaryName(str(resp.get("Name")));
                txn.setApiResponse(Json.write(resp));
                verificationTxnRepository.save(txn);
                return ok("SUCCESS", resp.get("Name") != null ? resp.get("Name") : "Verified", 200);
            }
            case "Failed" -> {
                txn.setStatus("FAILED");
                txn.setOperatorId(resp.get("status") != null ? str(resp.get("status")) : "Failed");
                txn.setApiResponse(Json.write(resp));
                verificationTxnRepository.save(txn);
                walletService.refund(user, txn.getAmount(), txn, service);
                return err("FAILED", resp.get("status") != null ? resp.get("status") : "Verification Failed", 403);
            }
            default -> {
                txn.setStatus("PENDING");
                txn.setOperatorId(status);
                txn.setApiResponse(Json.write(resp));
                verificationTxnRepository.save(txn);
                return ok("Transaction Under Process", resp, 202);
            }
        }
    }
}
