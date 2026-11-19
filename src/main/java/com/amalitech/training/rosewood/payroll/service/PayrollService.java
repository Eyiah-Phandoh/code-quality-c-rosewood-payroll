package com.amalitech.training.rosewood.payroll.service;

import com.amalitech.training.rosewood.payroll.deduction.DeductionCalculator;
import com.amalitech.training.rosewood.payroll.domain.Employee;
import com.amalitech.training.rosewood.payroll.domain.PayrollResult;
import com.amalitech.training.rosewood.payroll.exception.EmployeeNotFoundException;
import com.amalitech.training.rosewood.payroll.exception.InvalidPayrollInputException;
import com.amalitech.training.rosewood.payroll.exception.PayrollCalculationException;
import com.amalitech.training.rosewood.payroll.exception.PayrollException;
import com.amalitech.training.rosewood.payroll.repository.EmployeeRepository;
import com.amalitech.training.rosewood.payroll.tax.TaxCalculator;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates a payroll calculation from validated input to an auditable result.
 */
public final class PayrollService {

    private static final Logger LOGGER = Logger.getLogger(PayrollService.class.getName());

    private final EmployeeRepository employeeRepository;
    private final GrossPayCalculator grossPayCalculator;
    private final TaxCalculator taxCalculator;
    private final DeductionCalculator deductionCalculator;

    public PayrollService(
            EmployeeRepository employeeRepository,
            GrossPayCalculator grossPayCalculator,
            TaxCalculator taxCalculator,
            DeductionCalculator deductionCalculator) {
        this.employeeRepository = Objects.requireNonNull(
                employeeRepository, "employeeRepository must not be null");
        this.grossPayCalculator = Objects.requireNonNull(
                grossPayCalculator, "grossPayCalculator must not be null");
        this.taxCalculator = Objects.requireNonNull(
                taxCalculator, "taxCalculator must not be null");
        this.deductionCalculator = Objects.requireNonNull(
                deductionCalculator, "deductionCalculator must not be null");
    }

    /**
     * Calculates a complete payroll result for one employee.
     *
     * @param employeeId the employee identifier
     * @param hoursWorked non-negative hours worked
     * @param hourlyRate non-negative hourly rate
     * @return the gross, tax, deductions, and derived net pay
     * @throws InvalidPayrollInputException when an input is null, blank, or negative
     * @throws EmployeeNotFoundException when the employee does not exist
     * @throws PayrollCalculationException when a calculation cannot produce a trustworthy result
     */
    public PayrollResult calculatePayroll(
            String employeeId,
            BigDecimal hoursWorked,
            BigDecimal hourlyRate) {
        validateInputs(employeeId, hoursWorked, hourlyRate);

        try {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

            BigDecimal grossPay = grossPayCalculator.calculate(hoursWorked, hourlyRate);
            BigDecimal tax = taxCalculator.calculateTax(grossPay);
            BigDecimal deductions = deductionCalculator.calculateDeductions(employee, grossPay);

            return new PayrollResult(
                    employee.getId(),
                    employee.getName(),
                    hoursWorked,
                    hourlyRate,
                    grossPay,
                    tax,
                    deductions);
        } catch (PayrollException payrollFailure) {
            // Domain failures already carry context; wrapping them would change the contract.
            throw payrollFailure;
        } catch (RuntimeException exception) {
            LOGGER.log(
                    Level.SEVERE,
                    "Unexpected payroll failure for employee " + employeeId,
                    exception);
            throw new PayrollCalculationException(
                    "Could not calculate payroll for employee " + employeeId, exception);
        }
    }

    private static void validateInputs(
            String employeeId,
            BigDecimal hoursWorked,
            BigDecimal hourlyRate) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new InvalidPayrollInputException("Employee id must not be null or blank");
        }
        requireNonNegative(hoursWorked, "Hours worked");
        requireNonNegative(hourlyRate, "Hourly rate");
    }

    private static void requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new InvalidPayrollInputException(fieldName + " must not be null");
        }
        if (value.signum() < 0) {
            throw new InvalidPayrollInputException(fieldName + " must not be negative: " + value);
        }
    }
}
