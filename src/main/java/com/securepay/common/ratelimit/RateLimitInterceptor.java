package com.securepay.common.ratelimit;

import com.securepay.common.exception.ApiException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces {@link RateLimit} annotations per client+endpoint, returning HTTP 429 with the error
 * envelope message {@code Too Many Attempts.} (doc 07 §7.5, doc 08 §8.4).
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RateLimit limit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (limit == null) {
            return true;
        }

        Duration window = limit.seconds() > 0
                ? Duration.ofSeconds(limit.seconds())
                : Duration.ofMinutes(limit.minutes());

        String key = clientKey(request) + "|" + handlerMethod.getMethod().toGenericString();
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(limit.requests(), window));

        if (bucket.tryConsume(1)) {
            return true;
        }
        throw new ApiException("Too Many Attempts.", HttpStatus.TOO_MANY_REQUESTS);
    }

    private Bucket newBucket(int capacity, Duration window) {
        Bandwidth bandwidth = Bandwidth.classic(capacity, Refill.intervally(capacity, window));
        return Bucket.builder().addLimit(bandwidth).build();
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
