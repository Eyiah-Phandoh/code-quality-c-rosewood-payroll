package com.amalitech.training.rosewood.payroll.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Centralises the monetary precision policy for payroll calculations.
 */
public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);

    private Money() {
    }

    /**
     * Returns the amount rounded to the payroll scale using {@link #ROUNDING_MODE}.
     *
     * @param amount the exact amount to normalise
     * @return the amount at {@link #SCALE} decimal places
     */
    public static BigDecimal round(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        return amount.setScale(SCALE, ROUNDING_MODE);
    }
}
