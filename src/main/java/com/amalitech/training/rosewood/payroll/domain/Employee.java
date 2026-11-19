package com.amalitech.training.rosewood.payroll.domain;

import com.amalitech.training.rosewood.payroll.validation.Inputs;

/**
 * An employee who can be paid by the payroll service.
 */
public final class Employee {

    private final String id;
    private final String name;

    public Employee(String id, String name) {
        this.id = Inputs.requireText(id, "Employee id");
        this.name = Inputs.requireText(name, "Employee name");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
