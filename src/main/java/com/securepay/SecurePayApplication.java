package com.securepay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * SecurePay Spring Boot application.
 *
 * <p>Behaviour-identical replacement for the legacy PHP SecurePay fintech backend.
 * Same database, same endpoints, same response envelopes (see docs-springboot).</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
public class SecurePayApplication {

    public static void main(String[] args) {
        // Timezone fixed to Asia/Kolkata to match the legacy timestamp semantics (doc 02 §2.3, doc 08).
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(SecurePayApplication.class, args);
    }
}
