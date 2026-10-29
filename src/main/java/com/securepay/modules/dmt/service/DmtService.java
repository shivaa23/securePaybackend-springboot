package com.securepay.modules.dmt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.money.Money;
import com.securepay.modules.commission.dto.CommissionResult;
import com.securepay.modules.commission.service.AdminCommissionCalculator.AdminCommission;
import com.securepay.modules.dmt.dto.DmtTxnRequest;
import com.securepay.modules.dmt.entity.DmtTxn;
import com.securepay.modules.dmt.repository.DmtTxnRepository;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.user.entity.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Port of {@code DmtService::create} — the canonical CCF/GST/TDS/commission split (doc 10 §10.4).
 *
 * <p>⚠️ Verbatim parity caveats preserved (flagged with NOTE):</p>
 * <ul>
 *   <li>CCF: {@code amount > 1000 ? round(amount*1/100,2) : 10}.</li>
 *   <li>GST (inclusive): {@code ccf - ccf*100/118} (== ccf*18/118).</li>
 *   <li>ret_tds = 2% of ret_comm; di_tds/md_tds computed from the UNSET txn fields → 0.</li>
 *   <li>admin a_comm = residual: {@code ccf - gst - ret_comm - di_comm - md_comm - a_comm}.</li>
 * </ul>
 */
@Service
public class DmtService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ONE_EIGHTEEN = BigDecimal.valueOf(118);

    private final DmtTxnRepository dmtTxnRepository;
    private final ObjectMapper objectMapper;

    public DmtService(DmtTxnRepository dmtTxnRepository, ObjectMapper objectMapper) {
        this.dmtTxnRepository = dmtTxnRepository;
        this.objectMapper = objectMapper;
    }

    /** Port of {@code DmtService::create($user, $service, $request, $comm, $a_comm)}. */
    public DmtTxn create(User user, ServiceEntity service, DmtTxnRequest request,
                         CommissionResult comm, BigDecimal aComm, String userAgent, String ip) {
        DmtTxn txn = new DmtTxn();
        txn.setUserId(user.getId());
        txn.setTxnId("DMT" + epochSeconds() + rand(100, 999));
        txn.setRoute(service.getRoute());
        txn.setClientRef(request.getClientRef());
        txn.setPf(userAgent);
        txn.setSenderMobile(request.getMobileNumber());
        txn.setBeneficiaryName(request.getBenName());
        txn.setAccountNumber(request.getBenAcc());
        txn.setIfscCode(request.getIfsc());
        txn.setBankName(request.getBankName());
        txn.setMop(request.getType());
        txn.setOperator(service.getName());

        BigDecimal amount = Money.nz(request.getAmount());
        txn.setAmount(amount);

        // CCF: 1% if > 1000 else flat 10.
        BigDecimal ccf = amount.compareTo(BigDecimal.valueOf(1000)) > 0
                ? Money.round2(amount.multiply(BigDecimal.ONE).divide(HUNDRED, 10, RoundingMode.HALF_UP))
                : BigDecimal.TEN;
        // GST inclusive within CCF: ccf - ccf*100/118.
        BigDecimal gst = ccf.subtract(ccf.multiply(HUNDRED).divide(ONE_EIGHTEEN, 10, RoundingMode.HALF_UP));
        txn.setGst(gst);
        txn.setCcf(ccf);

        BigDecimal retComm = orZero(comm.getRet());
        BigDecimal ddComm = orZero(comm.getDd());
        BigDecimal diComm = orZero(comm.getDi());
        BigDecimal mdComm = orZero(comm.getMd());

        BigDecimal resolvedRetComm = "ret".equals(user.getRole()) ? retComm
                : ("dd".equals(user.getRole()) ? ddComm : BigDecimal.ZERO);
        txn.setRetComm(resolvedRetComm);
        txn.setDiComm(diComm);
        txn.setMdComm(mdComm);

        // TDS: ret_tds = 2% of ret_comm.
        txn.setRetTds(Money.round2(resolvedRetComm.multiply(BigDecimal.valueOf(2))
                .divide(HUNDRED, 10, RoundingMode.HALF_UP)));
        // NOTE: source reads txn.di_tds / txn.md_tds (unset → null → 0) rather than di_comm/md_comm.
        // Result: di_tds = md_tds = 0. Preserved verbatim (doc 10 §10.4).
        txn.setDiTds(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        txn.setMdTds(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        // Admin commission residual.
        BigDecimal adminComm = ccf.subtract(gst).subtract(retComm).subtract(diComm)
                .subtract(mdComm).subtract(Money.nz(aComm));
        txn.setAComm(adminComm);

        txn.setLocation(locationJson(request.getLatitude(), request.getLongitude(), ip, "log"));

        return dmtTxnRepository.save(txn);
    }

    private String locationJson(String lat, String lng, String ip, String lngKey) {
        Map<String, Object> loc = new LinkedHashMap<>();
        loc.put("lat", lat);
        loc.put(lngKey, lng);
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

    private static long epochSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    private static int rand(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /** Overload accepting the admin-commission record directly. */
    public DmtTxn create(User user, ServiceEntity service, DmtTxnRequest request,
                         CommissionResult comm, AdminCommission aComm, String userAgent, String ip) {
        BigDecimal a = aComm == null || aComm.aComm() == null ? BigDecimal.ZERO : aComm.aComm();
        return create(user, service, request, comm, a, userAgent, ip);
    }
}
