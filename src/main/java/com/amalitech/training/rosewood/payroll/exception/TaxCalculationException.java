package com.amalitech.training.rosewood.payroll.exception;

/**
 * Raised when no tax rule can be applied to a gross amount.
 */
public final class TaxCalculationException extends PayrollCalculationException {

    public TaxCalculationException(String message) {
        super(message);
    }

    public TaxCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
