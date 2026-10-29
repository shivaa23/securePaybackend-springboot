package com.securepay.modules.recharge.service;

import com.securepay.common.exception.ApiException;
import com.securepay.common.money.Money;
import com.securepay.integration.common.SwitchResult;
import com.securepay.integration.recharge.RechargeClient;
import com.securepay.modules.commission.dto.CommissionResult;
import com.securepay.modules.commission.service.AdminCommissionCalculator;
import com.securepay.modules.commission.service.AdminCommissionCalculator.AdminCommission;
import com.securepay.modules.commission.service.CommissionCalculator;
import com.securepay.modules.recharge.dto.RechargeRequest;
import com.securepay.modules.recharge.entity.RechargeTxn;
import com.securepay.modules.recharge.repository.RechargeTxnRepository;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.service.repository.ServiceRepository;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.wallet.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Port of {@code RechargeController@recharge} orchestration (doc 10 §10.8):
 * role gate → MPIN → service lookup → commission → balance check → persist txn → debit →
 * provider recharge → on FAILED refund. Operator routing mirrors {@code Recharge::recharge}
 * dispatch by the service row's provider code columns.
 */
@Service
public class RechargeTransactionService {

    private final ServiceRepository serviceRepository;
    private final CommissionCalculator commissionCalculator;
    private final AdminCommissionCalculator adminCommissionCalculator;
    private final RechargeService rechargeService;
    private final WalletService walletService;
    private final RechargeClient rechargeClient;
    private final RechargeTxnRepository rechargeTxnRepository;

    public RechargeTransactionService(ServiceRepository serviceRepository,
                                      CommissionCalculator commissionCalculator,
                                      AdminCommissionCalculator adminCommissionCalculator,
                                      RechargeService rechargeService,
                                      WalletService walletService,
                                      RechargeClient rechargeClient,
                                      RechargeTxnRepository rechargeTxnRepository) {
        this.serviceRepository = serviceRepository;
        this.commissionCalculator = commissionCalculator;
        this.adminCommissionCalculator = adminCommissionCalculator;
        this.rechargeService = rechargeService;
        this.walletService = walletService;
        this.rechargeClient = rechargeClient;
        this.rechargeTxnRepository = rechargeTxnRepository;
    }

    @Transactional
    public Map<String, Object> recharge(User user, RechargeRequest request, String userAgent, String ip) {
        // Role gate: api / dd / ret only.
        if (!List.of("api", "dd", "ret").contains(user.getRole())) {
            throw new ApiException("Action not Authorised", HttpStatus.FORBIDDEN);
        }

        ServiceEntity service = serviceRepository.findById(request.getOperator())
                .orElseThrow(() -> new ApiException("Service not found", HttpStatus.FORBIDDEN));

        BigDecimal amount = Money.nz(request.getAmount());

        CommissionResult comm = commissionCalculator.calculateCommission(user, service.getName(), amount);
        if (comm.isEmpty()) {
            throw new ApiException("Commission Slab Not Configured", HttpStatus.FORBIDDEN);
        }
        BigDecimal totalComm = comm.getRet() != null ? comm.getRet()
                : (comm.getDd() != null ? comm.getDd() : BigDecimal.ZERO);

        // Balance check: w1 (paise) >= (amount + lien + totalComm) * 100.
        long lienPaise = user.getLien() == null ? 0L : user.getLien();
        BigDecimal lienRupees = Money.toRupees(lienPaise);
        long totalRequiredPaise = Money.toPaise(amount.add(lienRupees).add(totalComm));
        long w1 = user.getW1() == null ? 0L : user.getW1();
        if (w1 < totalRequiredPaise) {
            throw new ApiException("Insufficient W1 Balance", HttpStatus.FORBIDDEN);
        }

        AdminCommission aCommRaw = adminCommissionCalculator
                .calculateAdminCommission(service.getName(), amount, service.getRoute());
        if (aCommRaw.isEmpty()) {
            throw new ApiException("Admin Commission Slab Not Configured", HttpStatus.FORBIDDEN);
        }
        BigDecimal aComm = adminCommissionCalculator.finalCommission(aCommRaw, amount);

        RechargeTxn txn = rechargeService.create(user, service, request, comm, aComm, userAgent, ip);
        walletService.debit(user, amount, txn, service);

        SwitchResult result = dispatch(service, txn);

        txn.setStatus(result.getStatus());
        txn.setOperatorId(result.getOperatorId());
        txn.setApiResponse(String.valueOf(result.getResponse()));
        rechargeTxnRepository.save(txn);

        if ("FAILED".equalsIgnoreCase(result.getStatus())) {
            walletService.refund(user, amount, txn, service);
        }
        return result.asMap();
    }

    /** Operator routing by the service row's provider code (port of {@code Recharge::recharge}). */
    private SwitchResult dispatch(ServiceEntity service, RechargeTxn txn) {
        RechargeClient.RechargeData d = new RechargeClient.RechargeData(
                txn.getMobileNumber(), txn.getAmount().toPlainString(), txn.getTxnId(), service.getSubType());

        if (notBlank(service.getGuru())) {
            return rechargeClient.guru(d, service.getGuru());
        }
        if (notBlank(service.getWellborn())) {
            return rechargeClient.wb(d, service.getWellborn());
        }
        if (notBlank(service.getShakti())) {
            return rechargeClient.shakti(d, service.getShakti());
        }
        if (notBlank(service.getMrobotics())) {
            return rechargeClient.mrb(d, service.getMrobotics());
        }
        if (notBlank(service.getAOne())) {
            return rechargeClient.aone(d, service.getAOne());
        }
        if (notBlank(service.getIndicore())) {
            return rechargeClient.indicore(d, service.getIndicore());
        }
        throw new ApiException("No recharge operator configured for this service", HttpStatus.FORBIDDEN);
    }

    private static boolean notBlank(String v) {
        return v != null && !v.isBlank() && !"0".equals(v);
    }
}
