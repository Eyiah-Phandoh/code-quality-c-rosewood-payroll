package com.amalitech.training.rosewood.payroll.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amalitech.training.rosewood.payroll.deduction.NoDeductionsCalculator;
import com.amalitech.training.rosewood.payroll.domain.Employee;
import com.amalitech.training.rosewood.payroll.domain.PayrollResult;
import com.amalitech.training.rosewood.payroll.tax.BandedTaxCalculator;
import com.amalitech.training.rosewood.payroll.tax.StandardTaxBands;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PayrollInvariantsTest {

    private final PayrollService service = new PayrollService(
            id -> Optional.of(new Employee("e1", "Efua Sarpong")),
            new StandardGrossPayCalculator(),
            new BandedTaxCalculator(StandardTaxBands.create()),
            new NoDeductionsCalculator());

    static Stream<Arguments> randomInputs() {
        Random random = new Random(42);
        return IntStream.range(0, 2_000).mapToObj(i -> Arguments.of(
                BigDecimal.valueOf(random.nextInt(20_001), 2),   // 0.00 - 200.00 hours
                BigDecimal.valueOf(random.nextInt(50_001), 2))); // 0.00 - 500.00 rate
    }

    @ParameterizedTest
    @MethodSource("randomInputs")
    void payslipAlwaysReconcilesWithTheInheritedRules(BigDecimal hours, BigDecimal rate) {
        // Oracle: the original starter formula, restated independently of production code.
        BigDecimal gross = hours.multiply(rate);
        if (hours.compareTo(new BigDecimal("40")) > 0) {
            gross = gross.add(hours.subtract(new BigDecimal("40"))
                    .multiply(rate).multiply(new BigDecimal("0.5")));
        }
        gross = gross.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxRate = gross.compareTo(new BigDecimal("1000")) < 0 ? new BigDecimal("0.05")
                : gross.compareTo(new BigDecimal("5000")) < 0 ? new BigDecimal("0.15")
                : new BigDecimal("0.25");
        BigDecimal expectedTax = gross.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedGross = gross;

        PayrollResult result = service.calculatePayroll("e1", hours, rate);

        assertAll(
                () -> assertEquals(expectedGross, result.grossPay()),
                () -> assertEquals(expectedTax, result.tax()),
                () -> assertEquals(
                        result.grossPay().subtract(result.tax()).subtract(result.deductions()),
                        result.netPay()),
                () -> assertTrue(result.netPay().signum() >= 0),
                () -> assertEquals(2, result.netPay().scale()));
    }
}
