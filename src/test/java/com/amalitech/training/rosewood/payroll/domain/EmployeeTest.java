package com.amalitech.training.rosewood.payroll.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amalitech.training.rosewood.payroll.exception.InvalidPayrollInputException;
import org.junit.jupiter.api.Test;

class EmployeeTest {

    @Test
    void exposesValidatedIdentity() {
        Employee employee = new Employee("e1", "Efua Sarpong");

        assertAll(
                () -> assertEquals("e1", employee.getId()),
                () -> assertEquals("Efua Sarpong", employee.getName()));
    }

    @Test
    void rejectsNullOrBlankId() {
        assertAll(
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> new Employee(null, "Efua Sarpong")),
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> new Employee("  ", "Efua Sarpong")));
    }

    @Test
    void rejectsNullOrBlankName() {
        assertAll(
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> new Employee("e1", null)),
                () -> assertThrows(
                        InvalidPayrollInputException.class,
                        () -> new Employee("e1", "  ")));
    }
}
