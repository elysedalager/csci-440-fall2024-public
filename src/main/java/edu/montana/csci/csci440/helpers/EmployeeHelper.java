package edu.montana.csci.csci440.helpers;

import edu.montana.csci.csci440.model.Employee;

import java.util.*;

public class EmployeeHelper {

    public static String makeEmployeeTree() {
        // TODO, change this to use a single query operation to get all employees
        Employee boss = Employee.find(1); // root boss
        // and use this data structure to maintain reference information needed to build the tree structure
        Map<Long, List<Employee>> employeeMap = new HashMap<>();

        List<Employee> all = Employee.all();
        for (Employee emp : all) {
            long reports = emp.getReportsTo();
            List<Employee> reportsTo = (employeeMap.get(reports) != null) ? employeeMap.get(reports) : new LinkedList<>();
            employeeMap.putIfAbsent(emp.getReportsTo(), reportsTo);
            reportsTo.add(emp);
        }

        return "<ul>" + makeTree(Objects.requireNonNull(boss), employeeMap) + "</ul>";
    }

    // TODO - currently this method just uses the employee.getReports() function, which
    //  issues a query.  Change that to use the employeeMap variable instead
    public static String makeTree(Employee employee, Map<Long, List<Employee>> employeeMap) {
        String list = "<li><a href='/employees/" + employee.getEmployeeId() + "'>"
                + employee.getEmail() + "</a><ul>";
        List<Employee> reports = employeeMap.get(employee.getEmployeeId());
        if (reports != null){
            for (Employee report : reports) {
                list += makeTree(report, employeeMap);
            }
        }
        return list + "</ul></li>";
    }
}
