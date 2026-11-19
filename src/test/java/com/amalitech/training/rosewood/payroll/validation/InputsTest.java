package com.amalitech.training.rosewood.payroll.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amalitech.training.rosewood.payroll.exception.InvalidPayrollInputException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InputsTest {

    @Test
    void rejectsNullAndBlankText() {
        assertAll(
                () -> assertRejectedContainsField(
                        () -> Inputs.requireText(null, "Employee name"), "Employee name"),
                () -> assertRejectedContainsField(
                        () -> Inputs.requireText("   ", "Employee name"), "Employee name"));
    }

    @Test
    void returnsValidTextUnchanged() {
        String value = "Efua Sarpong";

        assertSame(value, Inputs.requireText(value, "Employee name"));
    }

    @Test
    void rejectsNullAndNegativeAmounts() {
        assertAll(
                () -> assertRejectedContainsField(
                        () -> Inputs.requireNonNegative(null, "Hours worked"), "Hours worked"),
                () -> assertRejectedContainsField(
                        () -> Inputs.requireNonNegative(
                                new BigDecimal("-0.01"), "Hours worked"), "Hours worked"));
    }

    @Test
    void acceptsZeroAndPositiveAmounts() {
        assertAll(
                () -> assertEquals(
                        BigDecimal.ZERO,
                        Inputs.requireNonNegative(BigDecimal.ZERO, "Hours worked")),
                () -> assertEquals(
                        new BigDecimal("40"),
                        Inputs.requireNonNegative(new BigDecimal("40"), "Hours worked")));
    }

    private static void assertRejectedContainsField(Runnable rejection, String fieldName) {
        InvalidPayrollInputException exception =
                assertThrows(InvalidPayrollInputException.class, rejection::run);
        assertTrue(exception.getMessage().contains(fieldName));
    }
}
