package com.securepay.modules.payout.service;

import com.securepay.common.audit.AuditLogService;
import com.securepay.common.exception.ApiException;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.util.Json;
import com.securepay.common.web.Validation;
import com.securepay.modules.auditlog.entity.AuditLog;
import com.securepay.modules.auth.service.OtpService;
import com.securepay.modules.collection.entity.Collection2;
import com.securepay.modules.collection.repository.Collection2Repository;
import com.securepay.modules.commission.dto.CommissionResult;
import com.securepay.modules.commission.service.AdminCommissionCalculator;
import com.securepay.modules.commission.service.CommissionCalculator;
import com.securepay.modules.dmt.entity.VerificationTxn;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.service.repository.ServiceRepository;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.wallet.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utkal UPI (VPA) verification and dynamic-QR collection flows — the two operations that do not
 * share the bank payout pipeline of {@link PayoutFlow}.
 */
@Service
public class PayoutExtras {

    private static final List<String> ROLES = List.of("api", "dd", "ret");

    private final ServiceRepository serviceRepository;
    private final CommissionCalculator commissionCalculator;
    private final AdminCommissionCalculator adminCommissionCalculator;
    private final PayoutService payoutService;
    private final PayoutResponseHandler handler;
    private final PayoutProviders providers;
    private final WalletService walletService;
    private final AuditLogService auditLogService;
    private final OtpService otpService;
    private final Collection2Repository collection2Repository;
    private final ResponseFactory response;
    private final TransactionTemplate tx;

    public PayoutExtras(ServiceRepository serviceRepository, CommissionCalculator commissionCalculator,
                        AdminCommissionCalculator adminCommissionCalculator, PayoutService payoutService,
                        PayoutResponseHandler handler, PayoutProviders providers, WalletService walletService,
                        AuditLogService auditLogService, OtpService otpService,
                        Collection2Repository collection2Repository, ResponseFactory response,
                        PlatformTransactionManager transactionManager) {
        this.serviceRepository = serviceRepository;
        this.commissionCalculator = commissionCalculator;
        this.adminCommissionCalculator = adminCommissionCalculator;
        this.payoutService = payoutService;
        this.handler = handler;
        this.providers = providers;
        this.walletService = walletService;
        this.auditLogService = auditLogService;
        this.otpService = otpService;
        this.collection2Repository = collection2Repository;
        this.response = response;
        this.tx = new TransactionTemplate(transactionManager);
    }

    private static ApiException forbidden(String msg) {
        return new ApiException(msg, HttpStatus.FORBIDDEN, "");
    }

    /** Port of {@code UtkalController::upiVerification} (validation already done by the caller). */
    public ResponseEntity<Map<String, Object>> upiVerification(User user, PayoutCtx c) {
        if (!otpService.verifyMpin(user, c.s("mpin"))) {
            throw new ApiException("Invalid MPIN", HttpStatus.FORBIDDEN, List.of());
        }
        if (!ROLES.contains(user.getRole())) {
            throw forbidden("Action not Authorised");
        }
        ServiceEntity service = serviceRepository.findById(Long.valueOf(c.s("operator").trim())).orElse(null);
        if (service == null) {
            throw new ApiException("Invalid Service Selected", HttpStatus.NOT_FOUND, "");
        }
        BigDecimal one = BigDecimal.ONE;
        CommissionResult comm = commissionCalculator.calculateCommission(user, service.getName(), one);
        if (comm == null || comm.isEmpty()) {
            throw forbidden("Commission Slab Not Configured");
        }
        AdminCommissionCalculator.AdminCommission aRaw =
                adminCommissionCalculator.calculateAdminCommission(service.getName(), one, service.getRoute());
        if (aRaw == null || aRaw.isEmpty()) {
            throw forbidden("Admin Commission Slab Not Configured");
        }
        comm = commissionCalculator.finalCommission(comm, one, service);
        BigDecimal aComm = adminCommissionCalculator.finalCommission(aRaw, one);

        BigDecimal base = PayoutService.charge(comm) == null ? BigDecimal.ZERO : PayoutService.charge(comm);
        BigDecimal totalAmount = one.add(base).add(base.multiply(new BigDecimal("0.18")));
        BigDecimal need = one.add(totalAmount).multiply(BigDecimal.valueOf(100));
        if (BigDecimal.valueOf(user.getW1() == null ? 0L : user.getW1()).compareTo(need) < 0) {
            throw forbidden("Insufficient W1 Balance");
        }

        final CommissionResult commF = comm;
        record Init(VerificationTxn txn, AuditLog audit) {
        }
        Init init;
        try {
            init = tx.execute(st -> {
                VerificationTxn t = payoutService.createVerification(user, service, c, commF, aComm);
                walletService.debit(user, one, t, service);
                AuditLog a = auditLogService.logRequest(user.getId(), user.getRole(), service.getName(),
                        c.json(), c.ip(), c.s("client_ref"), c.userAgent());
                return new Init(t, a);
            });
        } catch (Exception e) {
            throw new ApiException("Verification Init Failed", HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        Map<String, Object> payload = providers.auth(true);
        payload.put("RequestID", init.txn().getTxnId());
        payload.put("ServiceId", 36);
        payload.put("VPA", c.s("account_number"));
        Map<String, Object> resp = providers.utkal(PayoutProviders.UT_UPI + "verify", payload);

        try {
            return tx.execute(st -> {
                ResponseEntity<Map<String, Object>> r = handler.handleUpiVerification(init.txn(), resp, user, service);
                auditLogService.updateResponse(init.audit().getId(), Json.write(resp));
                return r;
            });
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Transaction Update Failed", HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /** Port of {@code UtkalController::initiateDynamicQrCollection}. */
    public ResponseEntity<Map<String, Object>> initiateDynamicQr(User user, PayoutCtx c) {
        Validation v = Validation.of(c.p());
        v.field("email").required().email();
        v.field("amount").required().numeric().min(1);
        v.field("latitude").required();
        v.field("longitude").required();
        if (v.fails()) {
            return response.error("Validation failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!ROLES.contains(user.getRole())) {
            throw forbidden("Action not Authorised");
        }
        ServiceEntity service = serviceRepository.findById(366L).orElse(null);
        if (service == null) {
            throw forbidden("Service not Configured");
        }
        BigDecimal amount = c.amount();
        CommissionResult comm = commissionCalculator.calculateCommission(user, service.getName(), amount);
        if (comm == null || comm.isEmpty()) {
            throw forbidden("Commission Slab Not Configured");
        }
        AdminCommissionCalculator.AdminCommission aRaw =
                adminCommissionCalculator.calculateAdminCommission(service.getName(), amount, service.getRoute());
        if (aRaw == null || aRaw.isEmpty()) {
            throw forbidden("Admin Commission Slab Not Configured");
        }
        comm = commissionCalculator.finalCommission(comm, amount, service);
        BigDecimal aComm = adminCommissionCalculator.finalCommission(aRaw, amount);

        final CommissionResult commF = comm;
        record Init(Collection2 txn, AuditLog audit) {
        }
        Init init;
        try {
            init = tx.execute(st -> {
                Collection2 t = payoutService.createCollection(user, service, c, commF, aComm);
                AuditLog a = auditLogService.logRequest(user.getId(), user.getRole(), service.getName(),
                        c.json(), c.ip(), c.s("client_ref"), c.userAgent());
                return new Init(t, a);
            });
        } catch (Exception e) {
            throw new ApiException("Transaction Init Failed", HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        Map<String, Object> payload = providers.auth(true);
        payload.put("RequestID", init.txn().getTxnId());
        payload.put("ServiceId", "38");
        payload.put("Name", user.getEstablishment());
        payload.put("MerchantMobile", user.getMobile());
        payload.put("Emailid", c.s("email"));
        payload.put("Amount", c.s("amount"));
        payload.put("Latitude", c.s("latitude"));
        payload.put("Longitude", c.s("longitude"));
        Map<String, Object> resp = providers.utkal(PayoutProviders.UT_QR, payload);

        try {
            return tx.execute(st -> {
                auditLogService.updateResponse(init.audit().getId(), Json.write(resp));
                Collection2 t = init.txn();
                if (resp == null || resp.get("Status") == null) {
                    return response.error("Invalid API Response", resp, HttpStatus.INTERNAL_SERVER_ERROR);
                }
                if ("Pending".equals(resp.get("Status").toString())) {
                    t.setQrString(resp.get("QRString") == null ? null : resp.get("QRString").toString());
                    t.setApiResponse(Json.write(resp));
                    collection2Repository.save(t);
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("txn_id", t.getTxnId());
                    data.put("amount", resp.get("Amount"));
                    data.put("qr_string", resp.get("QRString"));
                    data.put("status", resp.get("Status"));
                    data.put("vendor_txn", resp.get("TransId"));
                    return response.success("QR Generated Successfully", data);
                }
                t.setStatus("FAILED");
                t.setApiResponse(Json.write(resp));
                collection2Repository.save(t);
                return response.error(resp.get("Description") != null ? resp.get("Description").toString()
                        : "Transaction Failed", resp, HttpStatus.BAD_REQUEST);
            });
        } catch (Exception e) {
            throw new ApiException("Transaction Update Failed", HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
