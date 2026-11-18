package com.amalitech.training.rosewood.payroll;

import com.amalitech.training.rosewood.payroll.deduction.NoDeductionsCalculator;
import com.amalitech.training.rosewood.payroll.domain.PayrollResult;
import com.amalitech.training.rosewood.payroll.exception.EmployeeNotFoundException;
import com.amalitech.training.rosewood.payroll.exception.PayrollException;
import com.amalitech.training.rosewood.payroll.repository.InMemoryEmployeeRepository;
import com.amalitech.training.rosewood.payroll.service.GrossPayCalculator;
import com.amalitech.training.rosewood.payroll.service.PayrollService;
import com.amalitech.training.rosewood.payroll.tax.BandedTaxCalculator;
import com.amalitech.training.rosewood.payroll.tax.StandardTaxBands;
import java.io.PrintStream;
import java.math.BigDecimal;

/**
 * Small command-line demonstration of the payroll service.
 */
public final class Demo {

    private Demo() {
    }

    public static void main(String[] args) {
        run(System.out);
    }

    static void run(PrintStream output) {
        run(output, createPayrollService());
    }

    static void run(PrintStream output, PayrollService payrollService) {
        printPayslip(payrollService, "e1", "40", "20", output);
        printPayslip(payrollService, "e2", "50", "30", output);
        printPayslip(payrollService, "unknown", "40", "20", output);
    }

    private static PayrollService createPayrollService() {
        return new PayrollService(
                InMemoryEmployeeRepository.withSampleData(),
                new GrossPayCalculator(),
                new BandedTaxCalculator(StandardTaxBands.create()),
                new NoDeductionsCalculator());
    }

    private static void printPayslip(
            PayrollService payrollService,
            String employeeId,
            String hoursWorked,
            String hourlyRate,
            PrintStream output) {
        try {
            PayrollResult result = payrollService.calculatePayroll(
                    employeeId,
                    new BigDecimal(hoursWorked),
                    new BigDecimal(hourlyRate));
            output.printf("Employee: %s (%s)%n", result.employeeName(), result.employeeId());
            output.printf("Gross pay: GHS %s%n", result.grossPay());
            output.printf("Tax: GHS %s%n", result.tax());
            output.printf("Deductions: GHS %s%n", result.deductions());
            output.printf("Net pay: GHS %s%n%n", result.netPay());
        } catch (EmployeeNotFoundException exception) {
            output.printf("Payroll failed: %s%n%n", exception.getMessage());
        } catch (PayrollException exception) {
            output.printf(
                    "Payroll failed for employee %s: %s%n%n",
                    employeeId,
                    exception.getMessage());
        }
    }
}
