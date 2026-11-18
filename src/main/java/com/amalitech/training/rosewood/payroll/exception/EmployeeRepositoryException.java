package com.amalitech.training.rosewood.payroll.exception;

/**
 * Raised when employee storage cannot satisfy a lookup request.
 */
public final class EmployeeRepositoryException extends PayrollException {

    public EmployeeRepositoryException(String message) {
        super(message);
    }

    public EmployeeRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
