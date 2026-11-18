package com.amalitech.training.rosewood.payroll.support;

import com.amalitech.training.rosewood.payroll.domain.Employee;
import java.util.List;

/**
 * Test-only copy of the demo employees, so tests do not reach into the composition root.
 */
public final class SampleEmployees {

    private static final Employee EFUA = new Employee("e1", "Efua Sarpong");
    private static final Employee KWAME = new Employee("e2", "Kwame Owusu");

    private SampleEmployees() {
    }

    public static List<Employee> all() {
        return List.of(EFUA, KWAME);
    }
}
