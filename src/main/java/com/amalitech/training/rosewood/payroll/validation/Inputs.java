package com.amalitech.training.rosewood.payroll.validation;

import com.amalitech.training.rosewood.payroll.exception.InvalidPayrollInputException;
import java.math.BigDecimal;

/**
 * Shared input checks so every class rejects bad input with the same exception and message.
 */
public final class Inputs {

    private Inputs() {
    }

    public static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidPayrollInputException(fieldName + " must not be null or blank");
        }
        return value;
    }

    public static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new InvalidPayrollInputException(fieldName + " must not be null");
        }
        if (value.signum() < 0) {
            throw new InvalidPayrollInputException(fieldName + " must not be negative: " + value);
        }
        return value;
    }
}
