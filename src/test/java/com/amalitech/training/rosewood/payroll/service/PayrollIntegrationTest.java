package com.amalitech.training.rosewood.payroll.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amalitech.training.rosewood.payroll.deduction.NoDeductionsCalculator;
import com.amalitech.training.rosewood.payroll.domain.PayrollResult;
import com.amalitech.training.rosewood.payroll.exception.EmployeeNotFoundException;
import com.amalitech.training.rosewood.payroll.repository.InMemoryEmployeeRepository;
import com.amalitech.training.rosewood.payroll.tax.BandedTaxCalculator;
import com.amalitech.training.rosewood.payroll.tax.StandardTaxBands;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PayrollIntegrationTest {

    private PayrollService payrollService;

    @BeforeEach
    void setUp() {
        payrollService = new PayrollService(
                InMemoryEmployeeRepository.withSampleData(),
                new StandardGrossPayCalculator(),
                new BandedTaxCalculator(StandardTaxBands.create()),
                new NoDeductionsCalculator());
    }

    @Test
    void calculatesStandardPayUsingRealCollaborators() {
        PayrollResult result = payrollService.calculatePayroll(
                "e1", new BigDecimal("40"), new BigDecimal("20"));

        assertAll(
                () -> assertEquals("Efua Sarpong", result.employeeName()),
                () -> assertEquals(new BigDecimal("800.00"), result.grossPay()),
                () -> assertEquals(new BigDecimal("40.00"), result.tax()),
                () -> assertEquals(new BigDecimal("0.00"), result.deductions()),
                () -> assertEquals(new BigDecimal("760.00"), result.netPay()));
    }

    @Test
    void calculatesOvertimeAndTaxUsingRealCollaborators() {
        PayrollResult result = payrollService.calculatePayroll(
                "e2", new BigDecimal("50"), new BigDecimal("30"));

        assertAll(
                () -> assertEquals("Kwame Owusu", result.employeeName()),
                () -> assertEquals(new BigDecimal("1650.00"), result.grossPay()),
                () -> assertEquals(new BigDecimal("247.50"), result.tax()),
                () -> assertEquals(new BigDecimal("1402.50"), result.netPay()));
    }

    @Test
    void calculatesHighBandPayUsingRealCollaborators() {
        PayrollResult result = payrollService.calculatePayroll(
                "e1", new BigDecimal("100"), new BigDecimal("100"));

        assertAll(
                () -> assertEquals(new BigDecimal("13000.00"), result.grossPay()),
                () -> assertEquals(new BigDecimal("3250.00"), result.tax()),
                () -> assertEquals(new BigDecimal("9750.00"), result.netPay()));
    }

    @Test
    void failsForUnknownEmployeeInsteadOfReturningZero() {
        assertThrows(
                EmployeeNotFoundException.class,
                () -> payrollService.calculatePayroll(
                        "unknown", new BigDecimal("40"), new BigDecimal("20")));
    }
}
