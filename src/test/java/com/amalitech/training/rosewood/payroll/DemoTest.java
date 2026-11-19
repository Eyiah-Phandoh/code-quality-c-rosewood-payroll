package com.amalitech.training.rosewood.payroll;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amalitech.training.rosewood.payroll.deduction.NoDeductionsCalculator;
import com.amalitech.training.rosewood.payroll.exception.TaxCalculationException;
import com.amalitech.training.rosewood.payroll.repository.InMemoryEmployeeRepository;
import com.amalitech.training.rosewood.payroll.service.PayrollService;
import com.amalitech.training.rosewood.payroll.service.StandardGrossPayCalculator;
import com.amalitech.training.rosewood.payroll.support.SampleEmployees;
import com.amalitech.training.rosewood.payroll.tax.TaxCalculator;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock(Resources.SYSTEM_OUT)
class DemoTest {

    private static final Logger SERVICE_LOGGER =
            Logger.getLogger(PayrollService.class.getName());

    private static boolean previousUseParentHandlers;

    private PrintStream originalOut;

    @BeforeAll
    static void silenceServiceLogs() {
        previousUseParentHandlers = SERVICE_LOGGER.getUseParentHandlers();
        SERVICE_LOGGER.setUseParentHandlers(false);
    }

    @AfterAll
    static void restoreServiceLogs() {
        SERVICE_LOGGER.setUseParentHandlers(previousUseParentHandlers);
    }

    @AfterEach
    void restoreSystemOut() {
        if (originalOut != null) {
            System.setOut(originalOut);
        }
    }

    @Test
    void printsPayslipsAndAnExplicitFailure() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        originalOut = System.out;
        System.setOut(capture);
        PayrollApplication.main(new String[0]);

        String output = bytes.toString(StandardCharsets.UTF_8);

        String expected = """
                Employee: Efua Sarpong (e1)
                Gross pay: GHS 800.00
                Tax: GHS 40.00
                Deductions: GHS 0.00
                Net pay: GHS 760.00

                Employee: Kwame Owusu (e2)
                Gross pay: GHS 1650.00
                Tax: GHS 247.50
                Deductions: GHS 0.00
                Net pay: GHS 1402.50

                Payroll failed: Employee not found: unknown

                """.replace("\n", System.lineSeparator());
        assertEquals(expected, output);
    }

    @Test
    void reportsAnUnexpectedPayrollFailure() {
        TaxCalculator failingTaxCalculator = grossPay -> {
            throw new TaxCalculationException("tax engine offline");
        };
        PayrollService failingService = serviceWithTaxCalculator(failingTaxCalculator);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8);

        Demo.run(capture, failingService);

        String output = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Payroll failed for employee e1: tax engine offline"));
    }

    @Test
    void reportsTheCauseOfAWrappedFailure() {
        TaxCalculator failingTaxCalculator = grossPay -> {
            throw new IllegalStateException("tax engine offline");
        };
        PayrollService failingService = serviceWithTaxCalculator(failingTaxCalculator);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8);

        Demo.run(capture, failingService);

        String output = bytes.toString(StandardCharsets.UTF_8);
        assertAll(
                () -> assertTrue(output.contains(
                        "Payroll failed for employee e1: "
                                + "Could not calculate payroll for employee e1")),
                () -> assertTrue(output.contains(
                        "Caused by: java.lang.IllegalStateException: tax engine offline"
                                + System.lineSeparator()
                                + System.lineSeparator())));
    }

    private static PayrollService serviceWithTaxCalculator(TaxCalculator taxCalculator) {
        return new PayrollService(
                new InMemoryEmployeeRepository(SampleEmployees.all()),
                new StandardGrossPayCalculator(),
                taxCalculator,
                new NoDeductionsCalculator());
    }
}
