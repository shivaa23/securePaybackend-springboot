package com.securepay.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson configuration tuned for response parity with the legacy {@code json_encode} output (doc 08 §8.3).
 *
 * <ul>
 *   <li>Do not escape non-ASCII (the legacy encoder uses unescaped unicode).</li>
 *   <li>Preserve field insertion order (do not sort alphabetically).</li>
 *   <li>Do not blanket-omit nulls; PHP includes them as {@code null}.</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper securePayObjectMapper() {
        return JsonMapper.builder()
                // Serialize java.time types (LocalDateTime on entities) as ISO strings, not timestamps.
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();
    }
}
