package com.amalitech.training.rosewood.payroll.exception;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

class PayrollExceptionHierarchyTest {

    @Test
    void everyDomainFailureCanBeCaughtAsPayrollException() {
        assertInstanceOf(PayrollException.class, new EmployeeNotFoundException("x"));
        assertInstanceOf(PayrollException.class, new InvalidPayrollInputException("x"));
        assertInstanceOf(PayrollException.class,
                new EmployeeRepositoryException("x", new IllegalStateException("cause")));
        assertInstanceOf(PayrollException.class, new PayrollCalculationException("x"));
    }

    @Test
    void taxFailuresAreCalculationFailures() {
        assertInstanceOf(PayrollCalculationException.class, new TaxCalculationException("x"));
    }
}
