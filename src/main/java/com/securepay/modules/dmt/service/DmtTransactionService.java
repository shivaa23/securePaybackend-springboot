package com.securepay.modules.dmt.service;

import com.securepay.common.exception.ApiException;
import com.securepay.modules.commission.dto.CommissionResult;
import com.securepay.modules.commission.service.AdminCommissionCalculator;
import com.securepay.modules.commission.service.AdminCommissionCalculator.AdminCommission;
import com.securepay.modules.commission.service.CommissionCalculator;
import com.securepay.modules.dmt.dto.DmtTxnRequest;
import com.securepay.modules.dmt.entity.DmtTxn;
import com.securepay.modules.dmt.repository.DmtTxnRepository;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.service.repository.ServiceRepository;
import com.securepay.modules.user.entity.User;
import com.securepay.integration.impsguru.ImpsguruClient;
import com.securepay.modules.wallet.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * End-to-end DMT transaction orchestration (doc 10 §10.8):
 * <ol>
 *   <li>Resolve service.</li>
 *   <li>Commission tiers ({@code CommRule}) + admin commission ({@code AcommRule}).</li>
 *   <li>Persist the txn with CCF/GST/TDS/commission splits ({@link DmtService}).</li>
 *   <li>Debit W1 under a pessimistic lock ({@link WalletService}).</li>
 *   <li>Call the provider (ImpsGuru).</li>
 *   <li>On provider failure → refund (compensating CR ledger row).</li>
 * </ol>
 * All steps run inside one transaction with the user row locked, so a failed provider call rolls
 * back the debit (or triggers a compensating refund) — mirroring the source flow.
 */
@Service
public class DmtTransactionService {

    private final ServiceRepository serviceRepository;
    private final CommissionCalculator commissionCalculator;
    private final AdminCommissionCalculator adminCommissionCalculator;
    private final DmtService dmtService;
    private final WalletService walletService;
    private final ImpsguruClient impsguruClient;
    private final DmtTxnRepository dmtTxnRepository;

    public DmtTransactionService(ServiceRepository serviceRepository,
                                 CommissionCalculator commissionCalculator,
                                 AdminCommissionCalculator adminCommissionCalculator,
                                 DmtService dmtService,
                                 WalletService walletService,
                                 ImpsguruClient impsguruClient,
                                 DmtTxnRepository dmtTxnRepository) {
        this.serviceRepository = serviceRepository;
        this.commissionCalculator = commissionCalculator;
        this.adminCommissionCalculator = adminCommissionCalculator;
        this.dmtService = dmtService;
        this.walletService = walletService;
        this.impsguruClient = impsguruClient;
        this.dmtTxnRepository = dmtTxnRepository;
    }

    @Transactional
    public Map<String, Object> dmtTxn(User user, String serviceName, DmtTxnRequest request,
                                      String userAgent, String ip) {
        ServiceEntity service = serviceRepository.findByName(serviceName)
                .orElseThrow(() -> new ApiException("Service not configured: " + serviceName));

        BigDecimal amount = request.getAmount() == null ? BigDecimal.ZERO : request.getAmount();

        // 2) commissions
        CommissionResult comm = commissionCalculator.calculateCommission(user, serviceName, amount);
        comm = commissionCalculator.finalCommission(comm, amount, service);
        AdminCommission aCommRaw = adminCommissionCalculator
                .calculateAdminCommission(serviceName, amount, service.getRoute());
        BigDecimal aComm = adminCommissionCalculator.finalCommission(aCommRaw, amount);

        // 3) persist txn with splits
        DmtTxn txn = dmtService.create(user, service, request, comm, aComm, userAgent, ip);

        // 4) debit W1 (lock + balance guards)
        walletService.debit(user, amount, txn, service);

        // 5) provider call
        ImpsguruClient.PayoutData payoutData = new ImpsguruClient.PayoutData(
                request.getMobileNumber(), amount.toPlainString(), request.getBenAcc(),
                request.getBenName(), request.getIfsc(), request.getType(), txn.getTxnId(),
                request.getIfsc(), request.getLatitude(), request.getLongitude());
        Map<String, Object> providerResult = impsguruClient.payout(payoutData);

        String status = String.valueOf(providerResult.get("status"));
        txn.setStatus(status);
        txn.setApiResponse(String.valueOf(providerResult.get("response")));
        Object opId = providerResult.get("operator_id");
        txn.setOperatorId(opId == null ? null : opId.toString());
        dmtTxnRepository.save(txn);

        // 6) refund on provider failure (compensating credit)
        if ("FAILED".equalsIgnoreCase(status)) {
            walletService.refund(user, amount, txn, service);
        }

        return providerResult;
    }
}
