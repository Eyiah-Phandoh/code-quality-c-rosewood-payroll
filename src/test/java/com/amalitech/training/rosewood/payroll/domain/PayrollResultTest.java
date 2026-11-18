package com.amalitech.training.rosewood.payroll.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amalitech.training.rosewood.payroll.exception.PayrollCalculationException;
import com.amalitech.training.rosewood.payroll.money.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PayrollResultTest {

    private static final String EMPLOYEE_ID = "e1";
    private static final String EMPLOYEE_NAME = "Efua Sarpong";
    private static final BigDecimal HOURS = new BigDecimal("40");
    private static final BigDecimal RATE = new BigDecimal("20");

    @Test
    void derivesNetPayFromReportedComponents() {
        PayrollResult result = result("800.00", "40.00", "0.00");

        assertEquals(new BigDecimal("760.00"), result.netPay());
    }

    @Test
    void roundsComponentsToThePayrollScale() {
        PayrollResult result = result("800.005", "40.004", "0.004");

        assertAll(
                () -> assertEquals(new BigDecimal("800.01"), result.grossPay()),
                () -> assertEquals(new BigDecimal("40.00"), result.tax()),
                () -> assertEquals(new BigDecimal("0.00"), result.deductions()),
                () -> assertEquals(new BigDecimal("760.01"), result.netPay()));
    }

    @Test
    void rejectsTaxAndDeductionsThatExceedGrossPay() {
        assertThrows(
                PayrollCalculationException.class,
                () -> result("800.00", "40.00", "760.01"));
    }

    @Test
    void rejectsNullAndNegativeHoursAndRate() {
        assertAll(
                () -> assertThrows(
                        PayrollCalculationException.class,
                        () -> new PayrollResult(
                                EMPLOYEE_ID,
                                EMPLOYEE_NAME,
                                null,
                                RATE,
                                new BigDecimal("800.00"),
                                new BigDecimal("40.00"),
                                Money.ZERO)),
                () -> assertThrows(
                        PayrollCalculationException.class,
                        () -> new PayrollResult(
                                EMPLOYEE_ID,
                                EMPLOYEE_NAME,
                                new BigDecimal("-0.01"),
                                RATE,
                                new BigDecimal("800.00"),
                                new BigDecimal("40.00"),
                                Money.ZERO)),
                () -> assertThrows(
                        PayrollCalculationException.class,
                        () -> new PayrollResult(
                                EMPLOYEE_ID,
                                EMPLOYEE_NAME,
                                HOURS,
                                null,
                                new BigDecimal("800.00"),
                                new BigDecimal("40.00"),
                                Money.ZERO)),
                () -> assertThrows(
                        PayrollCalculationException.class,
                        () -> new PayrollResult(
                                EMPLOYEE_ID,
                                EMPLOYEE_NAME,
                                HOURS,
                                new BigDecimal("-0.01"),
                                new BigDecimal("800.00"),
                                new BigDecimal("40.00"),
                                Money.ZERO)));
    }

    @Test
    void rejectsNullIdentity() {
        assertAll(
                () -> assertThrows(
                        PayrollCalculationException.class,
                        () -> new PayrollResult(
                                null,
                                EMPLOYEE_NAME,
                                HOURS,
                                RATE,
                                new BigDecimal("800.00"),
                                new BigDecimal("40.00"),
                                Money.ZERO)),
                () -> assertThrows(
                        PayrollCalculationException.class,
                        () -> new PayrollResult(
                                EMPLOYEE_ID,
                                null,
                                HOURS,
                                RATE,
                                new BigDecimal("800.00"),
                                new BigDecimal("40.00"),
                                Money.ZERO)));
    }

    @Test
    void rejectsNegativeMonetaryComponents() {
        assertAll(
                () -> assertThrows(
                        PayrollCalculationException.class,
                        () -> result("-0.01", "0.00", "0.00")),
                () -> assertThrows(
                        PayrollCalculationException.class,
                        () -> result("800.00", "-0.01", "0.00")),
                () -> assertThrows(
                        PayrollCalculationException.class,
                        () -> result("800.00", "0.00", "-0.01")));
    }

    @Test
    void rejectsMissingMonetaryComponents() {
        assertThrows(
                PayrollCalculationException.class,
                () -> new PayrollResult(
                        EMPLOYEE_ID,
                        EMPLOYEE_NAME,
                        HOURS,
                        RATE,
                        null,
                        Money.ZERO,
                        Money.ZERO));
    }

    @Test
    void rejectsBlankIdentity() {
        assertAll(
                () -> assertThrows(
                        PayrollCalculationException.class,
                        () -> new PayrollResult(
                                " ",
                                EMPLOYEE_NAME,
                                HOURS,
                                RATE,
                                new BigDecimal("800.00"),
                                new BigDecimal("40.00"),
                                Money.ZERO)),
                () -> assertThrows(
                        PayrollCalculationException.class,
                        () -> new PayrollResult(
                                EMPLOYEE_ID,
                                "",
                                HOURS,
                                RATE,
                                new BigDecimal("800.00"),
                                new BigDecimal("40.00"),
                                Money.ZERO)));
    }

    @Test
    void netPayKeepsTwoDecimalPlaces() {
        PayrollResult result = result("800.00", "40.00", "0.00");

        assertEquals(2, result.netPay().scale());
    }

    private static PayrollResult result(String gross, String tax, String deductions) {
        return new PayrollResult(
                EMPLOYEE_ID,
                EMPLOYEE_NAME,
                HOURS,
                RATE,
                new BigDecimal(gross),
                new BigDecimal(tax),
                new BigDecimal(deductions));
    }
}
