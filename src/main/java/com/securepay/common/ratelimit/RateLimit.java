package com.securepay.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Per-endpoint rate limit mirroring the source {@code throttle:x,y} (x requests per y minutes).
 * (doc 07 §7.5, doc 05 §5.3). Place on controller methods that were throttled in the route files.
 *
 * <p>Example: {@code @RateLimit(requests = 5, minutes = 1)} == {@code throttle:5,1}.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** Max requests in the window. */
    int requests();

    /** Window length in minutes. Fractional windows use {@link #seconds()} instead. */
    int minutes() default 1;

    /** Optional explicit window in seconds (overrides minutes when > 0), for fractional throttles. */
    int seconds() default 0;
}
