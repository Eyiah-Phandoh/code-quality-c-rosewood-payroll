package com.amalitech.training.rosewood.payroll.repository;

import com.amalitech.training.rosewood.payroll.domain.Employee;
import java.util.Optional;

/**
 * Provides employee records to the payroll service.
 */
public interface EmployeeRepository {

    /**
     * Finds an employee by identifier.
     *
     * @param id the employee identifier; must not be null
     * @return the matching employee, or an empty optional when no employee has that identifier
     * @throws com.amalitech.training.rosewood.payroll.exception.InvalidPayrollInputException
     *         when the identifier is null
     * @throws com.amalitech.training.rosewood.payroll.exception.EmployeeRepositoryException
     *         when the underlying storage cannot be read
     */
    Optional<Employee> findById(String id);
}
