package com.amalitech.training.rosewood.payroll.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amalitech.training.rosewood.payroll.domain.Employee;
import com.amalitech.training.rosewood.payroll.exception.InvalidPayrollInputException;
import com.amalitech.training.rosewood.payroll.support.SampleEmployees;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryEmployeeRepositoryTest {

    @Test
    void findsEmployeeById() {
        InMemoryEmployeeRepository repository = repositoryWithEmployees();

        Employee employee = repository.findById("e1").orElseThrow();

        assertAll(
                () -> assertEquals("e1", employee.getId()),
                () -> assertEquals("Efua Sarpong", employee.getName()));
    }

    @Test
    void returnsEmptyForUnknownEmployee() {
        InMemoryEmployeeRepository repository = repositoryWithEmployees();

        assertTrue(repository.findById("unknown").isEmpty());
    }

    @Test
    void usesSuppliedEmployeeData() {
        InMemoryEmployeeRepository repository = new InMemoryEmployeeRepository(List.of(
                new Employee("custom", "Custom Employee")));

        assertAll(
                () -> assertEquals(
                        "Custom Employee",
                        repository.findById("custom").orElseThrow().getName()),
                () -> assertFalse(repository.findById("e1").isPresent()));
    }

    @Test
    void rejectsDuplicateEmployeeIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InMemoryEmployeeRepository(List.of(
                        new Employee("e1", "First"),
                        new Employee("e1", "Second"))));
    }

    @Test
    void rejectsNullConfiguration() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new InMemoryEmployeeRepository(null)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new InMemoryEmployeeRepository(java.util.Arrays.asList(
                                new Employee("e1", "First"), null))));
    }

    @Test
    void rejectsNullLookupId() {
        InMemoryEmployeeRepository repository = repositoryWithEmployees();

        assertThrows(InvalidPayrollInputException.class, () -> repository.findById(null));
    }

    private static InMemoryEmployeeRepository repositoryWithEmployees() {
        return new InMemoryEmployeeRepository(SampleEmployees.all());
    }
}
