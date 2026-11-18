package com.amalitech.training.rosewood.payroll.exception;

/**
 * Raised when payroll input is missing, blank, negative, or otherwise unusable.
 */
public final class InvalidPayrollInputException extends PayrollException {

    public InvalidPayrollInputException(String message) {
        super(message);
    }
}
