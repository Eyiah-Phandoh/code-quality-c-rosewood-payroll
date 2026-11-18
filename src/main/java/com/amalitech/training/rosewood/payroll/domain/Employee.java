package com.amalitech.training.rosewood.payroll.domain;

import com.amalitech.training.rosewood.payroll.exception.InvalidPayrollInputException;

/**
 * An employee who can be paid by the payroll service.
 */
public final class Employee {

    private final String id;
    private final String name;

    public Employee(String id, String name) {
        this.id = requireText(id, "Employee id");
        this.name = requireText(name, "Employee name");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidPayrollInputException(fieldName + " must not be null or blank");
        }
        return value;
    }
}
