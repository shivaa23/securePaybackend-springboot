package com.securepay.config;

import com.securepay.modules.dmt.entity.DmtBank;
import com.securepay.modules.dmt.repository.DmtBankRepository;
import com.securepay.modules.payout.entity.PayoutBeneficiary;
import com.securepay.modules.payout.entity.PayoutSender;
import com.securepay.modules.payout.entity.Purpose;
import com.securepay.modules.payout.repository.PayoutBeneficiaryRepository;
import com.securepay.modules.payout.repository.PayoutSenderRepository;
import com.securepay.modules.payout.repository.PurposeRepository;
import com.securepay.modules.plan.entity.Plan;
import com.securepay.modules.plan.repository.PlanRepository;
import com.securepay.modules.template.entity.Template;
import com.securepay.modules.template.repository.TemplateRepository;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Local-only dummy data for the Payout module: services (explicit ids the code relies on),
 * a commission plan with slabs, the OTP mail template, purposes, banks and the test sender
 * {@code 9519509053} on every provider pipe with a few beneficiaries each.
 *
 * <p>Idempotent and guarded by {@code @Profile("local")}. Test accounts: an account number starting
 * with {@code FAIL} makes the simulated provider answer "failed", {@code PEND} answers "pending",
 * anything else succeeds.</p>
 */
@Component
@Order(2)
@Profile("local")
public class PayoutDevSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PayoutDevSeeder.class);
    public static final String SENDER_MOBILE = "9519509053";

    /** id, name, route, type */
    private static final Object[][] SERVICES = {
            {11L, "Vendor Payment 1", "IMPSGURU", "AMTCHG"},
            {12L, "UPI_PAYOUT", "IMPSGURU", "AMTCHG"},
            {21L, "UPI_VERIFICATION", "UTKAL", "AMTCHG"},
            {81L, "SOLITECK_PAYOUT", "SOLITECK", "AMTCHG"},
            {336L, "ABHAYAM_PAYOUT", "ABHAYAM", "AMTCHG"},
            {337L, "UTKAL_PAYOUT", "UTKAL", "AMTCHG"},
            {338L, "UTKAL_UPI_PAYOUT", "UTKAL", "AMTCHG"},
            {366L, "QR_COLLECTION", "UTKAL", "AMT"},
    };

    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final TemplateRepository templateRepository;
    private final PurposeRepository purposeRepository;
    private final DmtBankRepository dmtBankRepository;
    private final PayoutSenderRepository senderRepository;
    private final PayoutBeneficiaryRepository beneficiaryRepository;

    public PayoutDevSeeder(JdbcTemplate jdbc, UserRepository userRepository, PlanRepository planRepository,
                           TemplateRepository templateRepository, PurposeRepository purposeRepository,
                           DmtBankRepository dmtBankRepository, PayoutSenderRepository senderRepository,
                           PayoutBeneficiaryRepository beneficiaryRepository) {
        this.jdbc = jdbc;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.templateRepository = templateRepository;
        this.purposeRepository = purposeRepository;
        this.dmtBankRepository = dmtBankRepository;
        this.senderRepository = senderRepository;
        this.beneficiaryRepository = beneficiaryRepository;
    }

    @Override
    public void run(String... args) {
        User retailer = userRepository.findAll().stream().filter(u -> "ret".equals(u.getRole())).findFirst().orElse(null);
        if (retailer == null) {
            return;
        }
        seedServices();
        Long planId = seedPlan();
        for (User u : userRepository.findAll()) {
            if (u.getPlanId() == null) {
                u.setPlanId(planId);
                userRepository.save(u);
            }
        }
        seedRules(planId);
        seedTemplate();
        seedPurposes();
        seedBanks();
        seedSenders(retailer);
        log.warn("[PayoutDevSeeder] Payout dummy data ready (sender {})", SENDER_MOBILE);
    }

    private void seedServices() {
        for (Object[] s : SERVICES) {
            Integer exists = jdbc.queryForObject("select count(*) from services where id = ?", Integer.class, s[0]);
            if (exists != null && exists > 0) {
                jdbc.update("update services set is_active=1, is_active_api=1, is_active_users=1, route=?, type=? where id=?",
                        s[2], s[3], s[0]);
            } else {
                jdbc.update("insert into services (id, name, route, type, status, is_active, is_active_api, "
                        + "is_active_users, created_at, updated_at) values (?,?,?,?,?,1,1,1,now(),now())",
                        s[0], s[1], s[2], s[3], "1");
            }
        }
        // services.code is unique + required by service/createService; give seeded rows a code.
        jdbc.update("update services set code = concat('SVC', id) where code is null");
    }

    private Long seedPlan() {
        return planRepository.findAll().stream().findFirst().orElseGet(() -> {
            Plan p = new Plan();
            p.setName("Default Plan");
            p.setDescription("Local dev plan");
            p.setPlanId("PLAN1");
            return planRepository.save(p);
        }).getId();
    }

    private void seedRules(Long planId) {
        for (Object[] s : SERVICES) {
            String name = (String) s[1];
            String route = (String) s[2];
            boolean qr = "QR_COLLECTION".equals(name);
            Integer c = jdbc.queryForObject("select count(*) from comm_rules where plan_id=? and service_name=?",
                    Integer.class, planId, name);
            if (c != null && c == 0) {
                if (qr) {
                    jdbc.update("insert into comm_rules (plan_id, service_name, rule_type, value_type, comm_ret, "
                            + "comm_dd, comm_di, comm_md, min_amount, max_amount) values (?,?,?,?,?,?,?,?,0,0)",
                            planId, name, "commission", "percent", "0.5", "0.5", "0.1", "0.05");
                } else {
                    jdbc.update("insert into comm_rules (plan_id, service_name, rule_type, value_type, comm_ret, "
                            + "comm_dd, comm_di, comm_md, min_amount, max_amount) values (?,?,?,?,?,?,?,?,0,0)",
                            planId, name, "charge", "flat", "10", "10", "0.10", "0.05");
                }
            }
            Integer a = jdbc.queryForObject("select count(*) from acomm_rules where route=? and service_name=?",
                    Integer.class, route, name);
            if (a != null && a == 0) {
                jdbc.update("insert into acomm_rules (route, service_name, rule_type, value_type, a_comm, "
                        + "min_amount, max_amount) values (?,?,?,?,?,0,0)",
                        route, name, qr ? "commission" : "charge", "flat", qr ? "0" : "4");
            }
        }
    }

    private void seedTemplate() {
        if (templateRepository.findFirstByName("MOB_VERIFY").isEmpty()) {
            Template t = new Template();
            t.setName("MOB_VERIFY");
            t.setTempId("DEV-TEMPLATE-1");
            t.setVendor("EMAIL");
            t.setHeader("SECPAY");
            t.setMessage("Your SecurePay sender verification OTP is {$otp}. Do not share it with anyone.");
            t.setStatus("1");
            templateRepository.save(t);
        }
    }

    private void seedPurposes() {
        if (purposeRepository.count() == 0) {
            for (String[] p : new String[][]{{"SALARY", "Salary payment"}, {"VENDOR", "Vendor payment"},
                    {"RENT", "Rent"}, {"FAMILY", "Family support"}}) {
                Purpose pu = new Purpose();
                pu.setType(p[0]);
                pu.setDescription(p[1]);
                purposeRepository.save(pu);
            }
        }
    }

    private void seedBanks() {
        if (dmtBankRepository.count() == 0) {
            String[][] banks = {{"HDFC Bank", "HDFC", "HDFC0000001"}, {"State Bank of India", "SBIN", "SBIN0000001"},
                    {"ICICI Bank", "ICIC", "ICIC0000001"}, {"Axis Bank", "UTIB", "UTIB0000001"},
                    {"Kotak Mahindra Bank", "KKBK", "KKBK0000001"}, {"Punjab National Bank", "PUNB", "PUNB0000001"}};
            for (String[] b : banks) {
                DmtBank d = new DmtBank();
                d.setName(b[0]);
                d.setBankId(b[1]);
                d.setIfsc(b[2]);
                d.setPipe("INSTANTPAY");
                d.setStatus(1);
                dmtBankRepository.save(d);
            }
        }
    }

    private void seedSenders(User retailer) {
        // Pipe order matters: payments/v2/payOut picks the first active sender for the mobile.
        String[] pipes = {null, "SOLITECK", "ABHAYAM", "UTKAL"};
        for (String pipe : pipes) {
            PayoutSender s = senderRepository.findFirstByMobileNumberAndPipe(SENDER_MOBILE, pipe).orElseGet(() -> {
                PayoutSender n = new PayoutSender();
                n.setUserId(retailer.getId());
                n.setSenderName("Shivam Sender");
                n.setEmail("shivammca099@gmail.com");
                n.setMobileNumber(SENDER_MOBILE);
                n.setPipe(pipe);
                n.setKycStatus("verified");
                n.setIsActive(1);
                n.setRemLimit(new BigDecimal("200000"));
                return senderRepository.save(n);
            });
            if (pipe == null) {
                bene(s, "BANK", "Ramesh Kumar", "123456789012", "HDFC0001234", "HDFC Bank", null, null);
                bene(s, "BANK", "Fail Account", "FAIL12345678", "HDFC0001234", "HDFC Bank", null, null);
                bene(s, "BANK", "Pending Account", "PEND12345678", "HDFC0001234", "HDFC Bank", null, null);
                bene(s, "UPI", "Upi Receiver", "receiver@ybl", null, null, null, null);
            } else if ("SOLITECK".equals(pipe)) {
                bene(s, "SOLITECK", "Ramesh Kumar", "123456789012", "HDFC0001234", "HDFC Bank", "SOLB1001", null);
                bene(s, "SOLITECK", "Fail Account", "FAIL12345678", "HDFC0001234", "HDFC Bank", "FAILB1002", null);
            } else if ("ABHAYAM".equals(pipe)) {
                bene(s, "ABHAYAM", "Ramesh Kumar", "123456789012", "HDFC0001234", "HDFC Bank", null, "ABB1001");
                bene(s, "ABHAYAM", "Fail Account", "FAIL12345678", "HDFC0001234", "HDFC Bank", null, "FAILAB1002");
            } else {
                bene(s, "UTKAL", "Ramesh Kumar", "123456789012", "HDFC0001234", "HDFC Bank", null, "UTB1001");
                bene(s, "UTKAL", "Fail Account", "FAIL12345678", "HDFC0001234", "HDFC Bank", null, "FAILUT1002");
                bene(s, "UTKALUPI", "Upi Receiver", "receiver@ybl", null, null, null, null);
            }
        }
    }

    private void bene(PayoutSender sender, String type, String name, String account, String ifsc, String bank,
                      String soliteckId, String apiId) {
        if (beneficiaryRepository.findFirstBySenderIdAndAccountNumberAndType(sender.getId(), account, type).isPresent()) {
            return;
        }
        PayoutBeneficiary b = new PayoutBeneficiary();
        b.setSenderId(sender.getId());
        b.setBeneficiaryName(name);
        b.setAccountNumber(account);
        b.setIfscCode(ifsc);
        b.setBankName(bank);
        b.setType(type);
        b.setSoliteckId(soliteckId);
        b.setApiId(apiId);
        b.setMobileNumber(sender.getMobileNumber());
        b.setIsVerified(1);
        b.setIsActive(1);
        beneficiaryRepository.save(b);
    }
}
