package com.amalitech.training.rosewood.payroll.repository;

import com.amalitech.training.rosewood.payroll.domain.Employee;
import com.amalitech.training.rosewood.payroll.exception.InvalidPayrollInputException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory employee storage.
 */
public final class InMemoryEmployeeRepository implements EmployeeRepository {

    private final Map<String, Employee> employees;

    public InMemoryEmployeeRepository(Collection<Employee> employees) {
        if (employees == null) {
            throw new IllegalArgumentException("Employees must not be null");
        }

        Map<String, Employee> employeesById = new HashMap<>();
        for (Employee employee : employees) {
            if (employee == null) {
                throw new IllegalArgumentException("Employees must not contain null");
            }
            if (employeesById.putIfAbsent(employee.getId(), employee) != null) {
                throw new IllegalArgumentException("Duplicate employee id: " + employee.getId());
            }
        }
        this.employees = Map.copyOf(employeesById);
    }

    @Override
    public Optional<Employee> findById(String id) {
        if (id == null) {
            throw new InvalidPayrollInputException("Employee id must not be null");
        }
        return Optional.ofNullable(employees.get(id));
    }
}
