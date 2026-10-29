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

/**
 * Port of the {@code check.active} middleware: 403 when the authenticated user is inactive
 * ({@code is_active == 0}) (doc 07 §7.5).
 */
@Component
public class ActiveUserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUser currentUser) {
            User user = currentUser.user();
            if (user != null && user.getIsActive() != null && user.getIsActive() == 0) {
                throw new ApiException("Your account is inactive. Please contact support.", HttpStatus.FORBIDDEN);
            }
        }
        return true;
    }
}
