package com.securepay.modules.recharge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.money.Money;
import com.securepay.modules.commission.dto.CommissionResult;
import com.securepay.modules.recharge.dto.RechargeRequest;
import com.securepay.modules.recharge.entity.RechargeTxn;
import com.securepay.modules.recharge.repository.RechargeTxnRepository;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.user.entity.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Port of {@code RechargeService::create} (persist the recharge txn with commission/GST/TDS splits).
 *
 * <ul>
 *   <li>gst = round(amount * 0.18, 2).</li>
 *   <li>ret_comm = role ret → comm.ret, role dd → comm.dd, else 0.</li>
 *   <li>a_comm = a_comm - di_comm - md_comm - ret_comm (residual).</li>
 *   <li>di_tds = di_comm * 0.02; md_tds = md_comm * 0.02.</li>
 * </ul>
 */
@Service
public class RechargeService {

    private final RechargeTxnRepository repository;
    private final ObjectMapper objectMapper;

    public RechargeService(RechargeTxnRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public RechargeTxn create(User user, ServiceEntity service, RechargeRequest request,
                              CommissionResult comm, BigDecimal aComm, String userAgent, String ip) {
        RechargeTxn txn = new RechargeTxn();
        txn.setUserId(user.getId());
        txn.setTxnId("RCH" + (System.currentTimeMillis() / 1000L) + rand());
        txn.setRoute(service.getRoute());
        txn.setClientRef(request.getClientRef());
        txn.setPf(userAgent);
        txn.setMobileNumber(request.getMobileNumber());
        txn.setOperator(service.getName());

        BigDecimal amount = Money.nz(request.getAmount());
        txn.setAmount(amount);
        txn.setGst(Money.round2(amount.multiply(BigDecimal.valueOf(0.18))));

        BigDecimal retComm = "ret".equals(user.getRole()) ? orZero(comm.getRet())
                : ("dd".equals(user.getRole()) ? orZero(comm.getDd()) : BigDecimal.ZERO);
        BigDecimal diComm = orZero(comm.getDi());
        BigDecimal mdComm = orZero(comm.getMd());
        txn.setRetComm(retComm);
        txn.setDiComm(diComm);
        txn.setMdComm(mdComm);
        // a_comm residual.
        txn.setAComm(Money.nz(aComm).subtract(diComm).subtract(mdComm).subtract(retComm));
        // di_tds / md_tds = 2% of respective commission (source uses di_comm/md_comm here, unlike DMT).
        txn.setDiTds(diComm.multiply(BigDecimal.valueOf(0.02)).setScale(4, RoundingMode.HALF_UP));
        txn.setMdTds(mdComm.multiply(BigDecimal.valueOf(0.02)).setScale(4, RoundingMode.HALF_UP));

        txn.setLocation(locationJson(request.getLatitude(), request.getLongitude(), ip));
        txn.setStatus("PENDING");
        return repository.save(txn);
    }

    private String locationJson(String lat, String lng, String ip) {
        Map<String, Object> loc = new LinkedHashMap<>();
        loc.put("lat", lat);
        loc.put("log", lng);
        loc.put("ip", ip);
        try {
            return objectMapper.writeValueAsString(loc);
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
}
