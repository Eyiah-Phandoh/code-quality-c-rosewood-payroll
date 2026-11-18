package com.amalitech.training.rosewood.payroll.exception;

/**
 * Raised when the requested employee does not exist in the repository.
 */
public final class EmployeeNotFoundException extends PayrollException {

    private final String employeeId;

    public EmployeeNotFoundException(String employeeId) {
        super("Employee not found: " + employeeId);
        this.employeeId = employeeId;
    }

    public String getEmployeeId() {
        return employeeId;
    }
}
