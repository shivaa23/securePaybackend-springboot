package com.securepay.common.web.interceptor;

import com.securepay.common.exception.ApiException;
import com.securepay.common.web.DecryptedPayload;
import com.securepay.modules.risk.repository.BlackListedAccRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * Port of the {@code check.blacklist} middleware: a request carrying {@code acc_no} (or
 * {@code account_number}) of a black-listed account is rejected with 403
 * "Transaction blocked: Account is black-listed". Requests without an account number pass.
 */
@Component
public class BlackListInterceptor implements HandlerInterceptor {

    private final BlackListedAccRepository blackListedAccRepository;

    public BlackListInterceptor(BlackListedAccRepository blackListedAccRepository) {
        this.blackListedAccRepository = blackListedAccRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object attr = request.getAttribute(DecryptedPayload.REQUEST_ATTRIBUTE);
        if (!(attr instanceof DecryptedPayload payload)) {
            return true;
        }
        Object acc = payload.fields().get("acc_no");
        if (acc == null || acc.toString().isEmpty() || "0".equals(acc.toString())) {
            acc = payload.fields().get("account_number");
        }
        if (acc == null || acc.toString().isEmpty() || "0".equals(acc.toString())) {
            return true;
        }
        if (blackListedAccRepository.findByAccNo(acc.toString()).isPresent()) {
            throw new ApiException("Transaction blocked: Account is black-listed", HttpStatus.FORBIDDEN, List.of());
        }
        return true;
    }
}
