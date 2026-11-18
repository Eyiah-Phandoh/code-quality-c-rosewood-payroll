package com.amalitech.training.rosewood.payroll;

import com.amalitech.training.rosewood.payroll.deduction.NoDeductionsCalculator;
import com.amalitech.training.rosewood.payroll.domain.Employee;
import com.amalitech.training.rosewood.payroll.repository.InMemoryEmployeeRepository;
import com.amalitech.training.rosewood.payroll.service.PayrollService;
import com.amalitech.training.rosewood.payroll.service.StandardGrossPayCalculator;
import com.amalitech.training.rosewood.payroll.tax.BandedTaxCalculator;
import com.amalitech.training.rosewood.payroll.tax.StandardTaxBands;
import java.util.List;

/**
 * Composition root: builds the application's collaborators and starts the CLI.
 */
public final class PayrollApplication {

    private static final List<Employee> SAMPLE_EMPLOYEES = List.of(
            new Employee("e1", "Efua Sarpong"),
            new Employee("e2", "Kwame Owusu"));

    private PayrollApplication() {
    }

    public static void main(String[] args) {
        Demo.run(System.out, createPayrollService());
    }

    static PayrollService createPayrollService() {
        return new PayrollService(
                new InMemoryEmployeeRepository(SAMPLE_EMPLOYEES),
                new StandardGrossPayCalculator(),
                new BandedTaxCalculator(StandardTaxBands.create()),
                new NoDeductionsCalculator());
    }
}
