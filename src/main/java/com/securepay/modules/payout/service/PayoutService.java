package com.securepay.modules.payout.service;

import com.securepay.common.money.Money;
import com.securepay.common.util.Json;
import com.securepay.modules.collection.entity.Collection2;
import com.securepay.modules.collection.repository.Collection2Repository;
import com.securepay.modules.commission.dto.CommissionResult;
import com.securepay.modules.dmt.entity.VerificationTxn;
import com.securepay.modules.dmt.repository.VerificationTxnRepository;
import com.securepay.modules.payout.entity.PayoutTxn;
import com.securepay.modules.payout.repository.PayoutTxnRepository;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.user.entity.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Port of {@code Payout/Services/PayoutService} (create / createCollection) and
 * {@code DmtService::createVerification}.
 */
@Service
public class PayoutService {

    private static final BigDecimal GST_DIVISOR = new BigDecimal("1.18");
    private static final BigDecimal TDS = new BigDecimal("0.02");

    private final PayoutTxnRepository payoutTxnRepository;
    private final Collection2Repository collection2Repository;
    private final VerificationTxnRepository verificationTxnRepository;

    public PayoutService(PayoutTxnRepository payoutTxnRepository,
                         Collection2Repository collection2Repository,
                         VerificationTxnRepository verificationTxnRepository) {
        this.payoutTxnRepository = payoutTxnRepository;
        this.collection2Repository = collection2Repository;
        this.verificationTxnRepository = verificationTxnRepository;
    }

    /** {@code $comm['ret'] ?? $comm['dd']} (null when neither). */
    public static BigDecimal charge(CommissionResult comm) {
        return comm.getRet() != null ? comm.getRet() : comm.getDd();
    }

    private static String stamp(String prefix) {
        return prefix + (System.currentTimeMillis() / 1000L) + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private static BigDecimal gst(BigDecimal charges) {
        return charges.subtract(charges.divide(GST_DIVISOR, 6, RoundingMode.HALF_UP));
    }

    private static String location(PayoutCtx c) {
        Map<String, Object> loc = new LinkedHashMap<>();
        loc.put("lat", c.s("latitude"));
        loc.put("log", c.s("longitude"));
        loc.put("ip", c.ip());
        return Json.write(loc);
    }

    /** Port of {@code PayoutService::create}. */
    public PayoutTxn create(User user, ServiceEntity service, PayoutCtx c, CommissionResult comm,
                            BigDecimal aComm) {
        PayoutTxn t = new PayoutTxn();
        t.setUserId(user.getId());
        t.setTxnId(stamp("PMT"));
        t.setRoute(service.getRoute());
        t.setClientRef(c.s("client_ref"));
        t.setPf(c.userAgent());
        t.setMobileNumber(c.s("mobile_number"));
        t.setBeneficiaryName(c.s("beneficiary_name"));
        t.setAccountNumber(c.s("account_number"));
        t.setIfscCode(c.s("ifsc_code"));
        t.setBankName(c.s("bank_name"));
        t.setMop(c.s("mop"));
        t.setOperator(service.getName());
        BigDecimal charges = Money.nz(charge(comm));
        t.setCharges(charges);
        t.setAmount(c.amount());
        t.setPurpose(c.s("purpose"));
        t.setGst(gst(charges));
        t.setDiComm(Money.nz(comm.getDi()));
        t.setMdComm(Money.nz(comm.getMd()));
        t.setDiTds(t.getDiComm().multiply(TDS));
        t.setMdTds(t.getMdComm().multiply(TDS));
        t.setAComm(charges.subtract(Money.nz(aComm)).subtract(t.getDiComm()).subtract(t.getMdComm()));
        t.setLocation(location(c));
        t.setStatus("PENDING");
        return payoutTxnRepository.save(t);
    }

    /** Port of {@code PayoutService::createCollection} (QR collections, table collections2). */
    public Collection2 createCollection(User user, ServiceEntity service, PayoutCtx c, CommissionResult comm,
                                        BigDecimal aComm) {
        BigDecimal charge = Money.nz(charge(comm));
        boolean isCharge = "charge".equals(comm.getRuleType());
        boolean isCommission = "commission".equals(comm.getRuleType());
        Collection2 t = new Collection2();
        t.setUserId(user.getId());
        t.setTxnId(stamp("QR"));
        t.setClientRef(c.s("client_ref"));
        t.setRoute(service.getRoute());
        t.setPf(c.userAgent());
        t.setCustomerMobile(c.s("param2") != null ? c.s("param2") : user.getMobile());
        t.setAmount(c.amount());
        t.setCharges(isCharge ? charge : BigDecimal.ZERO);
        t.setRetComm(isCommission ? charge : BigDecimal.ZERO);
        t.setDiComm(Money.nz(comm.getDi()));
        t.setMdComm(Money.nz(comm.getMd()));
        t.setOperator(service.getName());
        t.setAComm(charge.subtract(Money.nz(aComm)));
        t.setGst(t.getCharges().subtract(t.getCharges().divide(GST_DIVISOR, 6, RoundingMode.HALF_UP)));
        t.setRetTds(t.getRetComm().multiply(new BigDecimal("2")).divide(new BigDecimal("100")));
        t.setDiTds(t.getDiComm().multiply(new BigDecimal("2")).divide(new BigDecimal("100")));
        t.setMdTds(t.getMdComm().multiply(new BigDecimal("2")).divide(new BigDecimal("100")));
        t.setStatus("PENDING");
        return collection2Repository.save(t);
    }

    /** Port of {@code DmtService::createVerification} (account / UPI verification, table verification_txns). */
    public VerificationTxn createVerification(User user, ServiceEntity service, PayoutCtx c, CommissionResult comm,
                                              BigDecimal aComm) {
        VerificationTxn t = new VerificationTxn();
        t.setUserId(user.getId());
        t.setTxnId(stamp("ACV"));
        t.setRoute(service.getRoute());
        t.setClientRef(c.s("client_ref"));
        t.setPf(c.userAgent());
        t.setSenderMobile(c.s("number") != null ? c.s("number") : c.s("mobile_number"));
        t.setBeneficiaryName(c.s("ben_name"));
        t.setAccountNumber(c.s("ben_acc"));
        t.setIfscCode(c.s("ifsc"));
        t.setBankName(c.s("bank_name"));
        t.setMop("ACV");
        t.setOperator(service.getName() != null ? service.getName() : "ACV");
        BigDecimal ret = Money.nz(charge(comm));
        t.setGst(ret.multiply(new BigDecimal("18")).divide(new BigDecimal("100")));
        t.setCharges(ret);
        t.setAComm(ret.subtract(Money.nz(aComm)));
        t.setAmount(BigDecimal.ONE);
        Map<String, Object> loc = new LinkedHashMap<>();
        loc.put("lat", c.s("latitude"));
        loc.put("lng", c.s("longitude"));
        loc.put("ip", c.ip());
        t.setLocation(Json.write(loc));
        t.setStatus("PENDING");
        return verificationTxnRepository.save(t);
    }
}
