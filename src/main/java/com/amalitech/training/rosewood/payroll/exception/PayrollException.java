package com.amalitech.training.rosewood.payroll.exception;

/**
 * Base type for failures raised by the payroll domain.
 */
public abstract class PayrollException extends RuntimeException {

    protected PayrollException(String message) {
        super(message);
    }

    protected PayrollException(String message, Throwable cause) {
        super(message, cause);
    }
}
