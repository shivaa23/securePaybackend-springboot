package com.securepay.config;

import com.securepay.common.ratelimit.RateLimitInterceptor;
import com.securepay.common.web.interceptor.ActiveUserInterceptor;
import com.securepay.common.web.interceptor.BlackListInterceptor;
import com.securepay.common.web.interceptor.IpWhitelistInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the middleware interceptors in the same logical order as the source chain
 * (doc 04 §4.1, doc 07 §7.5): rate-limit, active-user, IP whitelist.
 *
 * <p>Account blacklist/whitelist and service-time gates are applied per module (scoped to the
 * relevant path patterns) as those modules are ported.</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final ActiveUserInterceptor activeUserInterceptor;
    private final IpWhitelistInterceptor ipWhitelistInterceptor;
    private final BlackListInterceptor blackListInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor,
                        ActiveUserInterceptor activeUserInterceptor,
                        IpWhitelistInterceptor ipWhitelistInterceptor,
                        BlackListInterceptor blackListInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.activeUserInterceptor = activeUserInterceptor;
        this.ipWhitelistInterceptor = ipWhitelistInterceptor;
        this.blackListInterceptor = blackListInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/**");
        registry.addInterceptor(activeUserInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/auth/signIn", "/auth/secureLogin", "/signup/**",
                        "/webhook/**", "/actuator/**");
        registry.addInterceptor(ipWhitelistInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/auth/signIn", "/auth/secureLogin", "/signup/**",
                        "/webhook/**", "/actuator/**");
        // check.blacklist applies to the payout route groups only (payout/*, payments/v2/*, qr/*).
        registry.addInterceptor(blackListInterceptor)
                .addPathPatterns("/payout/**", "/payments/v2/**", "/qr/initiateDynamic");
    }
}
