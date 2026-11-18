package com.amalitech.training.rosewood.payroll.tax;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.amalitech.training.rosewood.payroll.deduction.NoDeductionsCalculator;
import com.amalitech.training.rosewood.payroll.domain.Employee;
import com.amalitech.training.rosewood.payroll.domain.PayrollResult;
import com.amalitech.training.rosewood.payroll.repository.EmployeeRepository;
import com.amalitech.training.rosewood.payroll.service.GrossPayCalculator;
import com.amalitech.training.rosewood.payroll.service.PayrollService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TaxBandExtensibilityTest {

    @Test
    void introducesANewBandWithoutChangingPayrollService() {
        TaxBand newBand = new TaxBand() {
            @Override
            public String name() {
                return "NEW_BAND";
            }

            @Override
            public boolean appliesTo(BigDecimal grossPay) {
                return grossPay.compareTo(new BigDecimal("10000")) >= 0;
            }

            @Override
            public BigDecimal calculateTax(BigDecimal grossPay) {
                return grossPay.multiply(new BigDecimal("0.30"));
            }
        };

        List<TaxBand> bands = List.of(
                new PercentageTaxBand("LOW", new BigDecimal("1000"), new BigDecimal("0.05")),
                new PercentageTaxBand("MEDIUM", new BigDecimal("5000"), new BigDecimal("0.15")),
                new PercentageTaxBand("HIGH", new BigDecimal("10000"), new BigDecimal("0.25")),
                newBand);

        EmployeeRepository repository = id -> Optional.of(
                new Employee("e3", "New Band Employee"));
        PayrollService payrollService = new PayrollService(
                repository,
                new GrossPayCalculator(),
                new BandedTaxCalculator(bands),
                new NoDeductionsCalculator());

        PayrollResult result = payrollService.calculatePayroll(
                "e3", new BigDecimal("120"), new BigDecimal("100"));

        assertAll(
                () -> assertEquals(new BigDecimal("16000.00"), result.grossPay()),
                () -> assertEquals(new BigDecimal("4800.00"), result.tax()),
                () -> assertEquals(new BigDecimal("11200.00"), result.netPay()));
    }
}
