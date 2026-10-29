package com.securepay.modules.commission.service;

import com.securepay.modules.commission.entity.AcommRule;
import com.securepay.modules.commission.repository.AcommRuleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Port of {@code AcommRule} static methods (doc 10 §10.5): admin commission leg.
 */
@Service
public class AdminCommissionCalculator {

    private final AcommRuleRepository acommRuleRepository;

    public AdminCommissionCalculator(AcommRuleRepository acommRuleRepository) {
        this.acommRuleRepository = acommRuleRepository;
    }

    /** Result of admin commission calc, mirroring {@code {a_comm, type, rule_type}}. */
    public record AdminCommission(BigDecimal aComm, String type, String ruleType, boolean present) {
        public static AdminCommission empty() {
            return new AdminCommission(null, null, null, false);
        }

        public boolean isEmpty() {
            return !present;
        }
    }

    /** Port of {@code AcommRule::calculateAdminCommission($service, $amount, $route)}. */
    public AdminCommission calculateAdminCommission(String service, BigDecimal amount, String route) {
        AcommRule rule = acommRuleRepository.findMatching(route, service, amount).orElse(null);
        if (rule == null) {
            return AdminCommission.empty();
        }
        BigDecimal value = rule.getAComm() == null ? BigDecimal.ZERO : rule.getAComm();
        BigDecimal commission = value.signum() > 0 ? value : BigDecimal.ZERO;
        return new AdminCommission(commission, rule.getValueType(), rule.getRuleType(), true);
    }

    /**
     * Port of {@code AcommRule::finalCommission($comm, $amount)}:
     * flat → a_comm; else → amount * a_comm / 100.
     */
    public BigDecimal finalCommission(AdminCommission comm, BigDecimal amount) {
        if ("flat".equals(comm.type())) {
            return comm.aComm() == null ? BigDecimal.ZERO : comm.aComm();
        }
        BigDecimal a = comm.aComm() == null ? BigDecimal.ZERO : comm.aComm();
        return amount.multiply(a).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
    }
}
