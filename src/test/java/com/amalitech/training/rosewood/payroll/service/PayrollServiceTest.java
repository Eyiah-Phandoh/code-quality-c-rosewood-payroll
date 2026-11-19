package com.amalitech.training.rosewood.payroll.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.amalitech.training.rosewood.payroll.deduction.DeductionCalculator;
import com.amalitech.training.rosewood.payroll.deduction.NoDeductionsCalculator;
import com.amalitech.training.rosewood.payroll.domain.Employee;
import com.amalitech.training.rosewood.payroll.domain.PayrollResult;
import com.amalitech.training.rosewood.payroll.exception.EmployeeNotFoundException;
import com.amalitech.training.rosewood.payroll.exception.EmployeeRepositoryException;
import com.amalitech.training.rosewood.payroll.exception.InvalidPayrollInputException;
import com.amalitech.training.rosewood.payroll.exception.PayrollCalculationException;
import com.amalitech.training.rosewood.payroll.exception.TaxCalculationException;
import com.amalitech.training.rosewood.payroll.repository.EmployeeRepository;
import com.amalitech.training.rosewood.payroll.support.FixedAmountDeductionCalculator;
import com.amalitech.training.rosewood.payroll.tax.BandedTaxCalculator;
import com.amalitech.training.rosewood.payroll.tax.StandardTaxBands;
import com.amalitech.training.rosewood.payroll.tax.TaxCalculator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    private static final BigDecimal FORTY_HOURS = new BigDecimal("40");
    private static final BigDecimal TWENTY_RATE = new BigDecimal("20");
    private static final Logger SERVICE_LOGGER =
            Logger.getLogger(PayrollService.class.getName());

    private static boolean previousUseParentHandlers;

    @BeforeAll
    static void silenceServiceLogs() {
        previousUseParentHandlers = SERVICE_LOGGER.getUseParentHandlers();
        SERVICE_LOGGER.setUseParentHandlers(false);
    }

    @AfterAll
    static void restoreServiceLogs() {
        SERVICE_LOGGER.setUseParentHandlers(previousUseParentHandlers);
    }

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TaxCalculator taxCalculatorMock;

    @Mock
    private DeductionCalculator deductionCalculatorMock;

    private GrossPayCalculator grossPayCalculator;
    private TaxCalculator taxCalculator;
    private DeductionCalculator deductionCalculator;
    private PayrollService payrollService;
    private Employee employee;

    @BeforeEach
    void setUp() {
        grossPayCalculator = new StandardGrossPayCalculator();
        taxCalculator = new BandedTaxCalculator(StandardTaxBands.create());
        deductionCalculator = new NoDeductionsCalculator();
        payrollService = new PayrollService(
                employeeRepository,
                grossPayCalculator,
                taxCalculator,
                deductionCalculator);
        employee = new Employee("e1", "Efua Sarpong");
    }

    @Test
    void rejectsNullCollaborators() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new PayrollService(
                                null,
                                grossPayCalculator,
                                taxCalculator,
                                deductionCalculator)),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new PayrollService(
                                employeeRepository,
                                null,
                                taxCalculator,
                                deductionCalculator)),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new PayrollService(
                                employeeRepository,
                                grossPayCalculator,
                                null,
                                deductionCalculator)),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new PayrollService(
                                employeeRepository,
                                grossPayCalculator,
                                taxCalculator,
                                null)));
    }

    @Test
    void calculatesStandardPayWithoutOvertime() {
        givenEmployee(employee);

        PayrollResult result = payrollService.calculatePayroll(
                "e1", FORTY_HOURS, TWENTY_RATE);

        assertAll(
                () -> assertEquals("e1", result.employeeId()),
                () -> assertEquals("Efua Sarpong", result.employeeName()),
                () -> assertEquals(new BigDecimal("800.00"), result.grossPay()),
                () -> assertEquals(new BigDecimal("40.00"), result.tax()),
                () -> assertEquals(new BigDecimal("0.00"), result.deductions()),
                () -> assertEquals(new BigDecimal("760.00"), result.netPay()));
        verify(employeeRepository).findById("e1");
    }

    @Test
    void calculatesZeroPayForZeroHours() {
        givenEmployee(employee);

        PayrollResult result = payrollService.calculatePayroll(
                "e1", BigDecimal.ZERO, TWENTY_RATE);

        assertAll(
                () -> assertEquals(new BigDecimal("0.00"), result.grossPay()),
                () -> assertEquals(new BigDecimal("0.00"), result.tax()),
                () -> assertEquals(new BigDecimal("0.00"), result.netPay()));
    }

    @Test
    void throwsWhenEmployeeDoesNotExist() {
        when(employeeRepository.findById("missing")).thenReturn(Optional.empty());

        EmployeeNotFoundException exception = assertThrows(
                EmployeeNotFoundException.class,
                () -> payrollService.calculatePayroll(
                        "missing", FORTY_HOURS, TWENTY_RATE));

        assertAll(
                () -> assertEquals("missing", exception.getEmployeeId()),
                () -> assertTrue(exception.getMessage().contains("missing")));
        verify(employeeRepository).findById("missing");
    }

    @ParameterizedTest(name = "rejects id={0}, hours={1}, rate={2}")
    @MethodSource("invalidInputs")
    void rejectsInvalidInputsBeforeRepositoryAccess(
            String employeeId,
            BigDecimal hoursWorked,
            BigDecimal hourlyRate) {
        assertThrows(
                InvalidPayrollInputException.class,
                () -> payrollService.calculatePayroll(employeeId, hoursWorked, hourlyRate));
        verifyNoInteractions(employeeRepository);
    }

    @Test
    void propagatesEmployeeRepositoryFailuresWithTheirCause() {
        EmployeeRepositoryException failure = new EmployeeRepositoryException(
                "Employee storage is unavailable",
                new IllegalStateException("database offline"));
        when(employeeRepository.findById("e1")).thenThrow(failure);

        EmployeeRepositoryException thrown = assertThrows(
                EmployeeRepositoryException.class,
                () -> payrollService.calculatePayroll("e1", FORTY_HOURS, TWENTY_RATE));

        assertAll(
                () -> assertSame(failure, thrown),
                () -> assertSame(failure.getCause(), thrown.getCause()));
        verify(employeeRepository).findById("e1");
    }

    @Test
    void wrapsUnexpectedRepositoryFailuresWithContextAndCause() {
        IllegalStateException failure = new IllegalStateException("database offline");
        when(employeeRepository.findById("e1")).thenThrow(failure);

        PayrollCalculationException thrown = assertThrows(
                PayrollCalculationException.class,
                () -> payrollService.calculatePayroll("e1", FORTY_HOURS, TWENTY_RATE));

        assertAll(
                () -> assertSame(failure, thrown.getCause()),
                () -> assertTrue(thrown.getMessage().contains("e1")));
        verify(employeeRepository).findById("e1");
    }

    @Test
    void logsUnexpectedFailuresWithEmployeeContextAndCause() {
        IllegalStateException failure = new IllegalStateException("database offline");
        when(employeeRepository.findById("e1")).thenThrow(failure);
        Logger logger = SERVICE_LOGGER;
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        boolean previousUseParentHandlers = logger.getUseParentHandlers();
        Level previousLevel = logger.getLevel();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.SEVERE);
        logger.addHandler(handler);
        try {
            assertThrows(
                    PayrollCalculationException.class,
                    () -> payrollService.calculatePayroll("e1", FORTY_HOURS, TWENTY_RATE));
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(previousUseParentHandlers);
            logger.setLevel(previousLevel);
        }

        assertEquals(1, records.size());
        LogRecord record = records.getFirst();
        assertAll(
                () -> assertEquals(Level.SEVERE, record.getLevel()),
                () -> assertTrue(record.getMessage().contains("e1")),
                () -> assertSame(failure, record.getThrown()));
    }

    @Test
    void propagatesTaxCalculationFailures() {
        TaxCalculator failingTaxCalculator = mock(TaxCalculator.class);
        TaxCalculationException failure = new TaxCalculationException("no tax band applies");
        when(failingTaxCalculator.calculateTax(any(BigDecimal.class))).thenThrow(failure);
        PayrollService serviceWithFailingTax = new PayrollService(
                employeeRepository,
                grossPayCalculator,
                failingTaxCalculator,
                deductionCalculator);
        givenEmployee(employee);

        TaxCalculationException thrown = assertThrows(
                TaxCalculationException.class,
                () -> serviceWithFailingTax.calculatePayroll(
                        "e1", FORTY_HOURS, TWENTY_RATE));

        assertSame(failure, thrown);
        verify(failingTaxCalculator).calculateTax(new BigDecimal("800.00"));
    }

    @Test
    void appliesDeductionsToNetPay() {
        DeductionCalculator fixedDeductions =
                new FixedAmountDeductionCalculator(new BigDecimal("100.00"));
        PayrollService serviceWithDeductions = new PayrollService(
                employeeRepository,
                grossPayCalculator,
                taxCalculator,
                fixedDeductions);
        givenEmployee(employee);

        PayrollResult result = serviceWithDeductions.calculatePayroll(
                "e1", FORTY_HOURS, TWENTY_RATE);

        assertAll(
                () -> assertEquals(new BigDecimal("800.00"), result.grossPay()),
                () -> assertEquals(new BigDecimal("40.00"), result.tax()),
                () -> assertEquals(new BigDecimal("100.00"), result.deductions()),
                () -> assertEquals(new BigDecimal("660.00"), result.netPay()));
    }

    @Test
    void rejectsDeductionsThatWouldMakeNetPayNegative() {
        DeductionCalculator excessiveDeductions =
                new FixedAmountDeductionCalculator(new BigDecimal("760.01"));
        PayrollService serviceWithExcessiveDeductions = new PayrollService(
                employeeRepository,
                grossPayCalculator,
                taxCalculator,
                excessiveDeductions);
        givenEmployee(employee);

        assertThrows(
                PayrollCalculationException.class,
                () -> serviceWithExcessiveDeductions.calculatePayroll(
                        "e1", FORTY_HOURS, TWENTY_RATE));
    }

    @Test
    void rejectsNullTaxResultAsACalculationFailure() {
        TaxCalculator nullTaxCalculator = grossPay -> null;
        PayrollService serviceWithNullTax = new PayrollService(
                employeeRepository,
                grossPayCalculator,
                nullTaxCalculator,
                deductionCalculator);
        givenEmployee(employee);

        PayrollCalculationException thrown = assertThrows(
                PayrollCalculationException.class,
                () -> serviceWithNullTax.calculatePayroll(
                        "e1", FORTY_HOURS, TWENTY_RATE));

        assertTrue(thrown.getMessage().contains("Tax"));
    }

    @Test
    void wrapsUnexpectedDeductionFailures() {
        IllegalStateException failure = new IllegalStateException("deduction engine offline");
        DeductionCalculator failingDeductions = (ignoredEmployee, ignoredGross) -> {
            throw failure;
        };
        PayrollService serviceWithFailingDeductions = new PayrollService(
                employeeRepository,
                grossPayCalculator,
                taxCalculator,
                failingDeductions);
        givenEmployee(employee);

        PayrollCalculationException thrown = assertThrows(
                PayrollCalculationException.class,
                () -> serviceWithFailingDeductions.calculatePayroll(
                        "e1", FORTY_HOURS, TWENTY_RATE));

        assertSame(failure, thrown.getCause());
    }

    @Test
    void netPayIsGrossMinusTaxMinusDeductionsAsReportedByCollaborators() {
        givenEmployee(employee);
        when(taxCalculatorMock.calculateTax(new BigDecimal("800.00")))
                .thenReturn(new BigDecimal("123.45"));
        when(deductionCalculatorMock.calculateDeductions(employee, new BigDecimal("800.00")))
                .thenReturn(new BigDecimal("67.89"));
        PayrollService service = new PayrollService(
                employeeRepository, grossPayCalculator, taxCalculatorMock, deductionCalculatorMock);

        PayrollResult result = service.calculatePayroll("e1", FORTY_HOURS, TWENTY_RATE);

        assertAll(
                () -> assertEquals(new BigDecimal("123.45"), result.tax()),
                () -> assertEquals(new BigDecimal("67.89"), result.deductions()),
                () -> assertEquals(new BigDecimal("608.66"), result.netPay()));
    }

    @Test
    void doesNotCalculateDeductionsWhenTaxFails() {
        givenEmployee(employee);
        when(taxCalculatorMock.calculateTax(any(BigDecimal.class)))
                .thenThrow(new TaxCalculationException("no band"));
        PayrollService service = new PayrollService(
                employeeRepository, grossPayCalculator, taxCalculatorMock, deductionCalculatorMock);

        assertThrows(TaxCalculationException.class,
                () -> service.calculatePayroll("e1", FORTY_HOURS, TWENTY_RATE));
        verifyNoInteractions(deductionCalculatorMock);
    }

    private void givenEmployee(Employee employee) {
        when(employeeRepository.findById(employee.getId()))
                .thenReturn(Optional.of(employee));
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of(null, FORTY_HOURS, TWENTY_RATE),
                Arguments.of("", FORTY_HOURS, TWENTY_RATE),
                Arguments.of("   ", FORTY_HOURS, TWENTY_RATE),
                Arguments.of("e1", null, TWENTY_RATE),
                Arguments.of("e1", new BigDecimal("-0.01"), TWENTY_RATE),
                Arguments.of("e1", FORTY_HOURS, null),
                Arguments.of("e1", FORTY_HOURS, new BigDecimal("-0.01")));
    }
}
