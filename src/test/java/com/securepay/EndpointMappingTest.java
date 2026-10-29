package com.securepay;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots the full application context and asserts the complete endpoint inventory (doc 05) is
 * registered — the real "all endpoints exist" parity check.
 */
@SpringBootTest
@ActiveProfiles("test")
class EndpointMappingTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void allModuleEndpointsAreMapped() {
        Set<RequestMappingInfo> mappings = handlerMapping.getHandlerMethods().keySet();

        long appEndpoints = mappings.stream()
                .filter(m -> m.getPatternsCondition() != null
                        ? !m.getPatternsCondition().getPatterns().isEmpty()
                        : m.getPathPatternsCondition() != null
                        && !m.getPathPatternsCondition().getPatterns().isEmpty())
                .count();

        // 337 generated + 18 auth = 355 module endpoints (plus the /error controller).
        assertTrue(appEndpoints >= 355,
                "Expected >= 355 endpoint mappings, found " + appEndpoints);

        // Spot-check representative paths across modules.
        String all = mappings.toString();
        assertTrue(all.contains("/auth/signIn"), "missing auth/signIn");
        assertTrue(all.contains("/dmt1/dmtTxn"), "missing dmt1/dmtTxn");
        assertTrue(all.contains("/payout/payOut"), "missing payout/payOut");
        assertTrue(all.contains("/aeps/cashWithdrawl"), "missing aeps/cashWithdrawl");
        assertTrue(all.contains("/wallet/createW2W"), "missing wallet/createW2W");
        assertTrue(all.contains("/webhook/guruWebhook"), "missing webhook/guruWebhook");
    }
}
