package com.amalitech.training.rosewood.payroll.exception;

/**
 * Raised when a payroll calculation cannot produce a trustworthy result.
 */
public class PayrollCalculationException extends PayrollException {

    public PayrollCalculationException(String message) {
        super(message);
    }

    public PayrollCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
