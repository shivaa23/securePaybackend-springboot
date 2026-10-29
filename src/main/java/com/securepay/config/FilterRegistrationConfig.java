package com.securepay.config;

import com.securepay.common.security.TokenAuthenticationFilter;
import com.securepay.common.web.PayloadDecryptionFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prevents Spring Boot from auto-registering our custom {@link jakarta.servlet.Filter} beans
 * directly in the servlet container's filter chain.
 *
 * <p>Both {@link PayloadDecryptionFilter} and {@link TokenAuthenticationFilter} are
 * {@code @Component}s extending {@code OncePerRequestFilter}. Boot would register such beans
 * at the servlet level (outside Spring Security) AND they are wired into the Security filter
 * chain. Because {@code OncePerRequestFilter} only runs once per request, the servlet-level
 * execution consumed that single run and set the SecurityContext — which the STATELESS
 * Security chain then cleared on its own {@code SecurityContextHolderFilter}, leaving the
 * request unauthenticated (401). Disabling the servlet-level registration makes these filters
 * run only inside the Security chain, where authentication actually sticks.</p>
 */
@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<PayloadDecryptionFilter> disablePayloadDecryptionFilter(
            PayloadDecryptionFilter filter) {
        FilterRegistrationBean<PayloadDecryptionFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<TokenAuthenticationFilter> disableTokenAuthenticationFilter(
            TokenAuthenticationFilter filter) {
        FilterRegistrationBean<TokenAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}
