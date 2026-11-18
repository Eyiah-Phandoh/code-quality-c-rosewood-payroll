package com.amalitech.training.rosewood.payroll;

import com.amalitech.training.rosewood.payroll.deduction.NoDeductionsCalculator;
import com.amalitech.training.rosewood.payroll.repository.InMemoryEmployeeRepository;
import com.amalitech.training.rosewood.payroll.service.PayrollService;
import com.amalitech.training.rosewood.payroll.service.StandardGrossPayCalculator;
import com.amalitech.training.rosewood.payroll.tax.BandedTaxCalculator;
import com.amalitech.training.rosewood.payroll.tax.StandardTaxBands;

/**
 * Composition root: builds the application's collaborators and starts the CLI.
 */
public final class PayrollApplication {

    private PayrollApplication() {
    }

    public static void main(String[] args) {
        Demo.run(System.out, createPayrollService());
    }

    static PayrollService createPayrollService() {
        return new PayrollService(
                InMemoryEmployeeRepository.withSampleData(),
                new StandardGrossPayCalculator(),
                new BandedTaxCalculator(StandardTaxBands.create()),
                new NoDeductionsCalculator());
    }
}
