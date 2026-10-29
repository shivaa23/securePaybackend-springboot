package com.securepay.modules.fundrequest.service;

import com.securepay.modules.account.entity.Account;
import com.securepay.modules.account.entity.AccountStatement;
import com.securepay.modules.account.repository.AccountRepository;
import com.securepay.modules.account.repository.AccountStatementRepository;
import com.securepay.modules.fundrequest.entity.FundRequest;
import com.securepay.modules.fundrequest.repository.FundRequestRepository;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import com.securepay.modules.wallet.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Transactional fund-request operations (port of FundRequestController::updateFundRequest).
 * Approval: credit the user's W1 wallet (ledger entry), bump the user's Account balance (respecting
 * the credit limit for non-sadm approvers) and write an AccountStatement — all in one transaction.
 */
@Service
public class FundRequestService {

    /** Business-rule failure carrying the HTTP status the controller should respond with. */
    public static class FundRequestException extends RuntimeException {
        private final int httpStatus;

        public FundRequestException(String message, int httpStatus) {
            super(message);
            this.httpStatus = httpStatus;
        }

        public int httpStatus() {
            return httpStatus;
        }
    }

    private final FundRequestRepository fundRequestRepository;
    private final AccountRepository accountRepository;
    private final AccountStatementRepository statementRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;

    public FundRequestService(FundRequestRepository fundRequestRepository,
                              AccountRepository accountRepository,
                              AccountStatementRepository statementRepository,
                              UserRepository userRepository,
                              WalletService walletService) {
        this.fundRequestRepository = fundRequestRepository;
        this.accountRepository = accountRepository;
        this.statementRepository = statementRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
    }

    /**
     * Approve or reject a pending fund request.
     *
     * @param approver the admin acting on the request (adm / sadm)
     * @param status   "approved" or "rejected"
     */
    @Transactional
    public FundRequest decide(User approver, Long fundRequestId, String status, String adminRemark) {
        FundRequest fr = fundRequestRepository.findById(fundRequestId)
                .orElseThrow(() -> new FundRequestException("Request Not Found", 403));

        if (!"pending".equals(fr.getStatus())) {
            throw new FundRequestException("FundRequest Already " + fr.getStatus().toUpperCase(), 400);
        }
        User owner = userRepository.findById(fr.getUserId())
                .orElseThrow(() -> new FundRequestException("User Not Found", 403));

        if ("approved".equals(status)) {
            Account account = accountRepository.findByUserId(fr.getUserId())
                    .orElseThrow(() -> new FundRequestException("Admin Account Not Found", 403));

            BigDecimal amount = fr.getAmount() == null ? BigDecimal.ZERO : fr.getAmount();
            BigDecimal balance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();

            // Non-sadm approvers cannot push the account past its credit limit.
            if (!"sadm".equals(approver.getRole()) && account.getCreditLimit() != null
                    && balance.add(amount).compareTo(account.getCreditLimit()) > 0) {
                throw new FundRequestException("Credit Limit Exceeded for this User", 400);
            }

            // 1) credit wallet W1 + wallet ledger row
            walletService.creditFundRequest(owner, amount, fr);

            // 2) bump account balance
            account.setBalance(balance.add(amount));
            accountRepository.save(account);

            // 3) account statement
            String note = "Fund Request Approved By " + approver.getName();
            AccountStatement st = new AccountStatement();
            st.setAccountId(account.getId());
            st.setCredit(amount);
            st.setDebit(BigDecimal.ZERO);
            st.setBalance(account.getBalance());
            st.setRemarks(note);
            st.setParticulars(note);
            st.setClaimedBy(approver.getId());
            statementRepository.save(st);

            fr.setStatus("approved");
        } else if ("rejected".equals(status)) {
            fr.setStatus("rejected");
        } else {
            throw new FundRequestException("Validation Failed", 422);
        }

        fr.setApprovedBy(approver.getName() != null ? approver.getName() : String.valueOf(approver.getId()));
        fr.setAdminRemark(adminRemark);
        return fundRequestRepository.save(fr);
    }
}
