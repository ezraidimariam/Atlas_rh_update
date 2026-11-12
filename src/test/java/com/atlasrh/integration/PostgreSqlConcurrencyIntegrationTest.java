package com.atlasrh.integration;

import com.atlasrh.dto.request.ContractRequest;
import com.atlasrh.dto.request.LeaveRequestCreate;
import com.atlasrh.entity.Department;
import com.atlasrh.entity.Employee;
import com.atlasrh.entity.LeaveRequest;
import com.atlasrh.entity.LeaveType;
import com.atlasrh.enums.ContractType;
import com.atlasrh.enums.RequestStatus;
import com.atlasrh.exception.BusinessRuleException;
import com.atlasrh.exception.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import com.atlasrh.repository.DepartmentRepository;
import com.atlasrh.repository.EmployeeRepository;
import com.atlasrh.repository.LeaveRequestRepository;
import com.atlasrh.repository.LeaveTypeRepository;
import com.atlasrh.service.ContractService;
import com.atlasrh.service.LeaveRequestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfSystemProperty(named = "atlasrh.postgres.integration", matches = "true")
class PostgreSqlConcurrencyIntegrationTest {

    @Autowired private ContractService contractService;
    @Autowired private LeaveRequestService leaveRequestService;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private LeaveTypeRepository leaveTypeRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUpTestData() {
        jdbcTemplate.update("""
                delete from leave_requests
                where employee_id in (select id from employees where employee_number like 'TEST-%')
                """);
        jdbcTemplate.update("""
                delete from contracts
                where employee_id in (select id from employees where employee_number like 'TEST-%')
                """);
        jdbcTemplate.update("delete from employees where upper(employee_number) like 'TEST-%'");
        jdbcTemplate.update("""
                delete from departments
                where description = 'Données d''intégration' and name like 'Test %'
                """);
    }

    @Test
    void nestedManagersCanOnlySeeAndDecideTheirDirectReports() {
        Employee topManager = createEmployee(null);
        Employee directManager = createEmployee(topManager);
        Employee indirectReport = createEmployee(directManager);
        Employee directReport = createEmployee(topManager);
        LeaveType leaveType = leaveTypeRepository.findAll().get(0);
        LocalDate firstDay = firstWeekday(LocalDate.now().getYear() + 2);
        Long indirectRequestId = createPendingRequest(indirectReport, leaveType, firstDay);
        Long directRequestId = createPendingRequest(directReport, leaveType, firstDay.plusWeeks(1));

        var directManagerRequests = leaveRequestService.pendingForManager(directManager.getId(),
                PageRequest.of(0, 20));
        var topManagerRequests = leaveRequestService.pendingForManager(topManager.getId(),
                PageRequest.of(0, 20));

        assertEquals(List.of(indirectRequestId),
                directManagerRequests.getContent().stream().map(request -> request.id()).toList());
        assertEquals(List.of(directRequestId),
                topManagerRequests.getContent().stream().map(request -> request.id()).toList());
        assertThrows(BusinessRuleException.class,
                () -> leaveRequestService.approve(topManager.getId(), indirectRequestId));
    }

    @Test
    void concurrentContractCreationCannotCreateTwoCurrentContracts() throws Exception {
        Employee employee = createEmployee(null);
        ContractRequest request = new ContractRequest(ContractType.CDI, LocalDate.now().minusDays(1),
                null, new java.math.BigDecimal("1000"));

        ConcurrentResult result = runTogether(() -> contractService.create(employee.getId(), request));

        assertEquals(1, result.successes());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0) instanceof ConflictException
                        || result.errors().get(0) instanceof DataIntegrityViolationException,
                "La concurrence doit être rejetée par le service ou par la contrainte PostgreSQL.");
    }

    @Test
    void concurrentApprovalsCannotExceedTheAnnualLeaveBalance() throws Exception {
        Employee manager = createEmployee(null);
        Employee employee = createEmployee(manager);
        LeaveType leaveType = leaveTypeRepository.findAll().stream()
                .filter(type -> type.getDaysPerYear() >= 2)
                .findFirst()
                .orElseThrow();

        LocalDate approvedStart = firstWeekday(LocalDate.now().getYear() + 1);
        LocalDate approvedEnd = dateAfterWorkingDays(approvedStart, leaveType.getDaysPerYear() - 1);
        LeaveRequest existingApproved = new LeaveRequest(employee, leaveType, approvedStart, approvedEnd,
                leaveType.getDaysPerYear() - 1, "Données de test concurrence");
        existingApproved.setStatus(RequestStatus.APPROUVEE);
        leaveRequestRepository.saveAndFlush(existingApproved);

        LocalDate firstPendingDay = nextWeekday(approvedEnd.plusDays(1));
        LocalDate secondPendingDay = nextWeekday(firstPendingDay.plusDays(1));
        Long firstRequestId = createPendingRequest(employee, leaveType, firstPendingDay);
        Long secondRequestId = createPendingRequest(employee, leaveType, secondPendingDay);

        ConcurrentResult result = runTogether(
                () -> leaveRequestService.approve(manager.getId(), firstRequestId),
                () -> leaveRequestService.approve(manager.getId(), secondRequestId));

        assertEquals(1, result.successes());
        assertEquals(1, result.errors().size());
        assertInstanceOf(BusinessRuleException.class, result.errors().get(0));
    }

    @Test
    void postgresRejectsCaseInsensitiveDuplicatesForUniqueFields() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String departmentName = "Test unique " + suffix;
        jdbcTemplate.update("insert into departments (name, description) values (?, ?)",
                departmentName, "Données d'intégration");
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "insert into departments (name, description) values (?, ?)",
                departmentName.toUpperCase(), "Données d'intégration"));

        Long departmentId = jdbcTemplate.queryForObject(
                "select id from departments where name = ?", Long.class, departmentName);
        insertEmployee("TEST-" + suffix, "unique-" + suffix + "@example.test", departmentId);
        assertThrows(DataIntegrityViolationException.class, () -> insertEmployee(
                "TEST-OTHER-" + suffix, "other-" + suffix + "@example.test", departmentId,
                "test-" + suffix.toLowerCase()));
        assertThrows(DataIntegrityViolationException.class, () -> insertEmployee(
                "TEST-EMAIL-" + suffix, ("unique-" + suffix + "@example.test").toUpperCase(),
                departmentId));
    }

    @Test
    void concurrentCaseInsensitiveDuplicateInsertsCannotBothSucceed() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        Long departmentId = departmentRepository.saveAndFlush(
                new Department("Test concurrent " + suffix, "Données d'intégration")).getId();

        assertOnlyOneConcurrentInsertSucceeds(
                () -> jdbcTemplate.update("insert into departments (name, description) values (?, ?)",
                        "Test race " + suffix, "Données d'intégration"),
                () -> jdbcTemplate.update("insert into departments (name, description) values (?, ?)",
                        "Test RACE " + suffix, "Données d'intégration"));
        assertOnlyOneConcurrentInsertSucceeds(
                () -> { insertEmployee("TEST-MAT-" + suffix, "race-a-" + suffix + "@example.test", departmentId); return null; },
                () -> { insertEmployee("TEST-mat-" + suffix, "race-b-" + suffix + "@example.test", departmentId); return null; });
        assertOnlyOneConcurrentInsertSucceeds(
                () -> { insertEmployee("TEST-EA-" + suffix, "same-" + suffix + "@example.test", departmentId); return null; },
                () -> { insertEmployee("TEST-EB-" + suffix, ("same-" + suffix + "@example.test").toUpperCase(), departmentId); return null; });
    }

    private void assertOnlyOneConcurrentInsertSucceeds(Callable<?> first, Callable<?> second) throws Exception {
        ConcurrentResult result = runTogether(first, second);
        assertEquals(1, result.successes());
        assertEquals(1, result.errors().size());
        assertInstanceOf(DataIntegrityViolationException.class, result.errors().get(0));
    }

    private void insertEmployee(String employeeNumber, String email, Long departmentId) {
        insertEmployee(employeeNumber, email, departmentId, employeeNumber);
    }

    private void insertEmployee(String employeeNumber, String email, Long departmentId, String matricule) {
        jdbcTemplate.update("""
                insert into employees (employee_number, last_name, first_name, email, position, hire_date, department_id)
                values (?, 'Test', 'Integration', ?, 'Test', current_date, ?)
                """, matricule, email, departmentId);
    }

    private Long createPendingRequest(Employee employee, LeaveType leaveType, LocalDate day) {
        return leaveRequestService.create(employee.getId(),
                new LeaveRequestCreate(leaveType.getId(), day, day, "Demande de test concurrence")).id();
    }

    private Employee createEmployee(Employee manager) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        Department department = departmentRepository.saveAndFlush(
                new Department("Test " + suffix, "Données d'intégration"));
        Employee employee = new Employee();
        employee.setEmployeeNumber("TEST-" + suffix);
        employee.setLastName("Integration");
        employee.setFirstName("Test");
        employee.setEmail(suffix + "@example.test");
        employee.setPosition("Test");
        employee.setHireDate(LocalDate.now().minusYears(1));
        employee.setDepartment(department);
        employee.setManager(manager);
        return employeeRepository.saveAndFlush(employee);
    }

    private ConcurrentResult runTogether(Callable<?> operation) throws Exception {
        return runTogether(operation, operation);
    }

    private ConcurrentResult runTogether(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return first.call();
            }));
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return second.call();
            }));
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Les deux opérations doivent être prêtes.");
            start.countDown();

            int successes = 0;
            List<Throwable> errors = new ArrayList<>();
            for (Future<?> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                    successes++;
                } catch (ExecutionException exception) {
                    errors.add(exception.getCause());
                }
            }
            return new ConcurrentResult(successes, errors);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Les opérations concurrentes doivent se terminer après l'arrêt des threads.");
        }
    }

    private LocalDate firstWeekday(int year) {
        return nextWeekday(LocalDate.of(year, 1, 1));
    }

    private LocalDate dateAfterWorkingDays(LocalDate start, int workingDays) {
        LocalDate date = start;
        int counted = 0;
        while (counted < workingDays) {
            if (isWeekday(date)) {
                counted++;
            }
            if (counted < workingDays) {
                date = date.plusDays(1);
            }
        }
        return date;
    }

    private LocalDate nextWeekday(LocalDate date) {
        while (!isWeekday(date)) {
            date = date.plusDays(1);
        }
        return date;
    }

    private boolean isWeekday(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    private record ConcurrentResult(int successes, List<Throwable> errors) {
    }
}
