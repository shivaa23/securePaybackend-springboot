package com.securepay.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money helpers enforcing the integer-paise convention (doc 10 §10.2).
 *
 * <ul>
 *   <li>Wallet columns are integer paise (long).</li>
 *   <li>Ledger rows store rupees (paise ÷ 100) as {@link BigDecimal} scale 2.</li>
 *   <li>Rounding matches PHP {@code round($x, 2)} → HALF_UP, 2dp (doc 10 §10.4).</li>
 *   <li>Never use {@code double} for money.</li>
 * </ul>
 */
public final class Money {

    private Money() {
    }

    /** Convert rupees → paise (× 100), rounding half-up then truncating to a whole paise. */
    public static long toPaise(BigDecimal rupees) {
        if (rupees == null) {
            return 0L;
        }
        return rupees.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /** Convert paise → rupees (÷ 100) as BigDecimal scale 2. */
    public static BigDecimal toRupees(long paise) {
        return BigDecimal.valueOf(paise).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** PHP-style round($x, 2): HALF_UP to 2 decimal places. */
    public static BigDecimal round2(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
