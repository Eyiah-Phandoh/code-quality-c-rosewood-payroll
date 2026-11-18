package com.amalitech.training.rosewood.payroll.money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

    @ParameterizedTest(name = "{0} rounds to {1}")
    @CsvSource({
            "1.004, 1.00",
            "1.005, 1.01",
            "1.006, 1.01",
            "2.675, 2.68",
            "-1.005, -1.01"
    })
    void roundsHalfUpToTwoDecimalPlaces(BigDecimal amount, BigDecimal expected) {
        assertEquals(expected, Money.round(amount));
    }

    @Test
    void normalisesWholeAmountsToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("800.00"), Money.round(new BigDecimal("800")));
    }

    @Test
    void zeroConstantHasTwoDecimalPlaces() {
        assertEquals(new BigDecimal("0.00"), Money.ZERO);
        assertEquals(2, Money.ZERO.scale());
    }

    @Test
    void rejectsNullAmount() {
        assertThrows(NullPointerException.class, () -> Money.round(null));
    }
}
