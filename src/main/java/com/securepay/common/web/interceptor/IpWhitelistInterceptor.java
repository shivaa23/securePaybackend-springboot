package com.securepay.common.web.interceptor;

import com.securepay.common.exception.ApiException;
import com.securepay.common.security.CurrentUser;
import com.securepay.modules.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * Port of the {@code whitelist.ip} middleware (doc 07 §7.5):
 * <ul>
 *   <li>role {@code api}: request IP must be in {@code whitelisted_ips}, else 403.</li>
 *   <li>other roles: enforced only if {@code whitelisted_ips} is set.</li>
 * </ul>
 */
@Component
public class IpWhitelistInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CurrentUser currentUser)) {
            return true;
        }
        User user = currentUser.user();
        if (user == null) {
            return true;
        }

        String whitelisted = user.getWhitelistedIps();
        boolean isApiRole = "api".equalsIgnoreCase(user.getRole());

        boolean hasWhitelist = whitelisted != null && !whitelisted.isBlank();
        if (!isApiRole && !hasWhitelist) {
            // Non-api roles without a whitelist are unrestricted.
            return true;
        }
        if (isApiRole && !hasWhitelist) {
            throw new ApiException("IP not whitelisted.", HttpStatus.FORBIDDEN);
        }

        List<String> allowed = Arrays.stream(whitelisted.split("[,\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        String clientIp = clientIp(request);
        if (!allowed.contains(clientIp)) {
            throw new ApiException("IP not whitelisted.", HttpStatus.FORBIDDEN);
        }
        return true;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
