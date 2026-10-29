package com.securepay.modules.commission.service;

import com.securepay.modules.commission.dto.CommissionResult;
import com.securepay.modules.commission.entity.CommRule;
import com.securepay.modules.commission.repository.CommRuleRepository;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Port of {@code CommRule} static methods (doc 10 §10.5): tier-walking commission calculation and
 * final normalization. Preserves source quirks verbatim (flagged with NOTE comments).
 */
@Service
public class CommissionCalculator {

    private final CommRuleRepository commRuleRepository;
    private final UserRepository userRepository;

    public CommissionCalculator(CommRuleRepository commRuleRepository, UserRepository userRepository) {
        this.commRuleRepository = commRuleRepository;
        this.userRepository = userRepository;
    }

    /** Port of {@code CommRule::calculateCommission($user, $service, $amount)}. */
    public CommissionResult calculateCommission(User user, String service, BigDecimal amount) {
        CommissionResult result = CommissionResult.empty();

        // Plan resolution (upline-first): md → di → user.
        User di = user.getParent() == null ? null : userRepository.findById(user.getParent()).orElse(null);
        User md = (di != null && di.getParent() != null)
                ? userRepository.findById(di.getParent()).orElse(null) : null;

        Long planId;
        if (md != null && md.getPlanId() != null) {
            planId = md.getPlanId();
        } else if (di != null && di.getPlanId() != null) {
            planId = di.getPlanId();
        } else {
            planId = user.getPlanId();
        }

        CommRule rule = commRuleRepository.findMatching(planId, service, amount).orElse(null);
        if (rule == null) {
            return result; // empty → no commission configured
        }

        // dd role special case: compute only comm_dd and return immediately.
        if ("dd".equals(user.getRole())) {
            BigDecimal value = Money.nz(rule.getCommDd());
            BigDecimal commission = BigDecimal.ZERO;
            if (value.signum() > 0) {
                commission = "flat".equals(rule.getValueType())
                        ? value : amount.multiply(value).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
            }
            result.setDd(commission);
            result.setType(rule.getValueType());
            result.setRuleType(rule.getRuleType());
            result.markPresent();
            return result;
        }

        // Other roles: walk up the hierarchy filling the matching tier each iteration.
        User cursor = user;
        while (cursor != null) {
            BigDecimal value;
            switch (cursor.getRole() == null ? "" : cursor.getRole()) {
                case "ret" -> value = rule.getCommRet();
                case "di" -> value = rule.getCommDi();
                case "md" -> value = rule.getCommMd();
                case "dd" -> value = rule.getCommDd();
                default -> value = BigDecimal.ZERO;
            }
            value = Money.nz(value);

            BigDecimal commission = BigDecimal.ZERO;
            if (value.signum() > 0) {
                commission = "flat".equals(rule.getValueType())
                        ? value : amount.multiply(value).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
            }
            assignTier(result, cursor.getRole(), commission);
            result.setType(rule.getValueType());
            result.setRuleType(rule.getRuleType());
            result.markPresent();

            cursor = cursor.getParent() == null ? null
                    : userRepository.findById(cursor.getParent()).orElse(null);
        }
        return result;
    }

    /**
     * Port of {@code CommRule::finalCommission($comm, $amount, $service)}.
     * ⚠️ Quirk: when type == 'flat', di/md are RE-computed as a percentage of amount (verbatim).
     * Services {24,50} bypass re-derivation.
     */
    public CommissionResult finalCommission(CommissionResult comm, BigDecimal amount, ServiceEntity service) {
        Long sid = service == null ? null : service.getId();
        if (sid != null && (sid == 24 || sid == 50)) {
            return comm; // no re-derivation
        }
        if ("flat".equals(comm.getType())) {
            if (comm.hasDi()) {
                comm.setDi(amount.multiply(comm.getDi()).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
            }
            if (comm.hasMd()) {
                comm.setMd(amount.multiply(comm.getMd()).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
            }
        }
        return comm;
    }

    private void assignTier(CommissionResult result, String role, BigDecimal commission) {
        switch (role == null ? "" : role) {
            case "ret" -> result.setRet(commission);
            case "di" -> result.setDi(commission);
            case "md" -> result.setMd(commission);
            case "dd" -> result.setDd(commission);
            default -> {
                // unknown role: source still sets $commissions[$user->role]; ignored here.
            }
        }
    }

    /** Local null-safe to avoid importing Money just for this. */
    private static final class Money {
        static BigDecimal nz(BigDecimal v) {
            return v == null ? BigDecimal.ZERO : v;
        }
    }
}
