package com.amalitech.training.rosewood.payroll.deduction;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amalitech.training.rosewood.payroll.domain.Employee;
import com.amalitech.training.rosewood.payroll.money.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NoDeductionsCalculatorTest {

    private final NoDeductionsCalculator calculator = new NoDeductionsCalculator();
    private final Employee employee = new Employee("e1", "Efua Sarpong");

    @Test
    void returnsZeroDeductions() {
        assertEquals(
                Money.ZERO,
                calculator.calculateDeductions(employee, new BigDecimal("800.00")));
    }

    @Test
    void rejectsNullArguments() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> calculator.calculateDeductions(null, new BigDecimal("800.00"))),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> calculator.calculateDeductions(employee, null)));
    }
}
