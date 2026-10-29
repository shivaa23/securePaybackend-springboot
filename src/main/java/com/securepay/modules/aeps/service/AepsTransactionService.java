package com.securepay.modules.aeps.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.exception.ApiException;
import com.securepay.common.money.Money;
import com.securepay.integration.ipay.IpayClient;
import com.securepay.modules.aeps.dto.AepsRequest;
import com.securepay.modules.aeps.entity.AepsTxn;
import com.securepay.modules.aeps.repository.AepsTxnRepository;
import com.securepay.modules.commission.dto.CommissionResult;
import com.securepay.modules.commission.service.CommissionCalculator;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.service.repository.ServiceRepository;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.wallet.service.WalletService2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AEPS cash-withdrawal orchestration — port of {@code AepsController@cashWithdrawl} (doc 10 §10.8).
 *
 * <ol>
 *   <li>Role gate (api/dd/ret), service + commission resolution.</li>
 *   <li>Persist the AEPS txn with commission/TDS splits.</li>
 *   <li>Call iPay cash-withdrawal.</li>
 *   <li>On SUCCESS → credit the withdrawn amount to W2 (settlement) via {@link WalletService2}.</li>
 * </ol>
 *
 * <p>NOTE: the Aadhaar-encryption + iPay payload envelope is proprietary to iPay; here we build the
 * plaintext request fields and call the provider. The exact {@code encryptAadhaar} byte format is a
 * follow-up once the iPay spec/keys are available (doc 09 Phase 4).</p>
 */
@Service
public class AepsTransactionService {

    private final ServiceRepository serviceRepository;
    private final CommissionCalculator commissionCalculator;
    private final WalletService2 walletService2;
    private final IpayClient ipayClient;
    private final AepsTxnRepository aepsTxnRepository;
    private final ObjectMapper objectMapper;

    public AepsTransactionService(ServiceRepository serviceRepository,
                                  CommissionCalculator commissionCalculator,
                                  WalletService2 walletService2,
                                  IpayClient ipayClient,
                                  AepsTxnRepository aepsTxnRepository,
                                  ObjectMapper objectMapper) {
        this.serviceRepository = serviceRepository;
        this.commissionCalculator = commissionCalculator;
        this.walletService2 = walletService2;
        this.ipayClient = ipayClient;
        this.aepsTxnRepository = aepsTxnRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> cashWithdrawl(User user, AepsRequest request, String userAgent, String ip) {
        if (!List.of("api", "dd", "ret").contains(user.getRole())) {
            throw new ApiException("Action not Authorised", HttpStatus.FORBIDDEN);
        }
        if (request.getAadhaarNumber() == null || request.getAadhaarNumber().isBlank()) {
            throw new ApiException("AadharNumber is required", HttpStatus.BAD_REQUEST);
        }
        ServiceEntity service = serviceRepository.findById(request.getOperator())
                .orElseThrow(() -> new ApiException("Invalid Operator/Service", HttpStatus.BAD_REQUEST));

        BigDecimal amount = Money.nz(request.getAmount());
        CommissionResult comm = commissionCalculator.calculateCommission(user, service.getName(), amount);
        comm = commissionCalculator.finalCommission(comm, amount, service);

        AepsTxn txn = createCashWithdraw(user, service, request, comm, ip);

        // Provider call (iPay cash withdrawal). Payload is the plaintext request fields.
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", amount.toPlainString());
        payload.put("aadhaarNumber", request.getAadhaarNumber());
        payload.put("mobileNumber", request.getMobileNumber());
        payload.put("bankIin", request.getBankIin());
        payload.put("clientRefId", txn.getTxnId());
        payload.put("latitude", request.getLatitude());
        payload.put("longitude", request.getLongitude());
        Map<String, Object> response = ipayClient.cashWithdrawl(write(payload));

        String statuscode = str(response.get("statuscode"));
        String status = mapStatus(statuscode);
        txn.setStatus(status);
        txn.setApiResponse(String.valueOf(response));
        aepsTxnRepository.save(txn);

        // On success credit the withdrawn amount to W2 (settlement wallet).
        if ("SUCCESS".equals(status)) {
            walletService2.credit(user, amount, txn, service);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("response", response);
        return out;
    }

    /** Port of {@code AepsService::createCashWithdraw}. */
    private AepsTxn createCashWithdraw(User user, ServiceEntity service, AepsRequest request,
                                       CommissionResult comm, String ip) {
        AepsTxn txn = new AepsTxn();
        txn.setUserId(user.getId());
        txn.setTxnId("AEPS" + (System.currentTimeMillis() / 1000L) + rand());
        txn.setRoute(service.getRoute());
        txn.setClientRef(request.getClientRef());
        txn.setAadhaarNumber(request.getAadhaarNumber());
        txn.setMobileNumber(request.getMobileNumber());
        txn.setBankIin(request.getBankIin());
        txn.setOperator(service.getName());
        txn.setAmount(Money.nz(request.getAmount()));

        BigDecimal retComm = "ret".equals(user.getRole()) ? orZero(comm.getRet())
                : ("dd".equals(user.getRole()) ? orZero(comm.getDd()) : BigDecimal.ZERO);
        txn.setRetComm(retComm);
        txn.setDiComm(orZero(comm.getDi()));
        txn.setMdComm(orZero(comm.getMd()));
        txn.setRetTds(Money.round2(retComm.multiply(BigDecimal.valueOf(0.02))));
        txn.setDiTds(BigDecimal.ZERO);
        txn.setMdTds(BigDecimal.ZERO);
        txn.setStatus("PENDING");

        Map<String, Object> loc = new LinkedHashMap<>();
        loc.put("lat", request.getLatitude());
        loc.put("log", request.getLongitude());
        loc.put("ip", ip);
        txn.setLocation(write(loc));
        return aepsTxnRepository.save(txn);
    }

    private String mapStatus(String statuscode) {
        if (statuscode == null) {
            return "PENDING";
        }
        return switch (statuscode) {
            case "TXN" -> "SUCCESS";
            case "ERR", "VAL" -> "FAILED";
            default -> "PENDING";
        };
    }

    private String write(Map<String, Object> m) {
        try {
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static int rand() {
        return ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
