//package sierra.tms.service;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import java.lang.reflect.Field;
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import org.junit.Before;
//import org.junit.Test;
//import sierra.tms.dao.ContractDao;
//import sierra.tms.dao.PersonDao;
//import sierra.tms.dao.TimesheetDao;
//import sierra.tms.dto.ContractDto;
//import sierra.tms.entities.ContractEntity;
//import sierra.tms.entities.PersonEntity;
//import sierra.tms.entities.RoleEntity;
//import sierra.tms.entities.TimesheetEntity;
//import sierra.tms.exceptions.BusinessRuleViolationException;
//import sierra.tms.exceptions.InvalidStateTransitionException;
//import sierra.tms.exceptions.TerminationWarning;
//import sierra.tms.exceptions.ValidationException;
//import sierra.tms.services.impl.ContractServiceImpl;
//import sierra.tms.utils.enums.ContractStatus;
//import sierra.tms.utils.enums.Frequency;
//import sierra.tms.utils.enums.RoleType;
//import sierra.tms.utils.enums.TimeSheetStatus;
//
///** CN5/CN6 contract workflow and custom-exception tests. */
//public class ContractServiceTest {
//
//    private ContractServiceImpl service;
//    private InMemoryContractDao contractDao;
//    private InMemoryTimesheetDao timesheetDao;
//    private InMemoryPersonDao personDao;
//
//    @Before
//    public void setUp() throws Exception {
//        service = new ContractServiceImpl();
//        contractDao = new InMemoryContractDao();
//        timesheetDao = new InMemoryTimesheetDao();
//        personDao = new InMemoryPersonDao();
//        inject(service, "contractDao", contractDao);
//        inject(service, "timesheetDao", timesheetDao);
//        inject(service, "personDao", personDao);
//
//        PersonEntity employee = person(1L, RoleType.EMPLOYEE);
//        PersonEntity supervisor = person(2L, RoleType.SUPERVISOR);
//        personDao.people.put(1L, employee);
//        personDao.people.put(2L, supervisor);
//    }
//
//    @Test
//    public void createContractCreatesPreparedContract() {
//        service.createContract(validDto());
//
//        assertNotNull(contractDao.saved);
//        assertEquals(ContractStatus.PREPARED, contractDao.saved.getStatus());
//        assertEquals("UI test contract", contractDao.saved.getName());
//    }
//
//    @Test
//    public void createContractRequiresAName() {
//        ContractDto dto = validDto();
//        dto.setName(" ");
//
//        ValidationException exception = expect(ValidationException.class, () -> service.createContract(dto));
//        assertEquals("error.contract.nameRequired", exception.getMessageKey());
//    }
//
//    @Test
//    public void createContractRequiresFirstDayOfMonthStartDate() {
//        ContractDto dto = validDto();
//        dto.setStartDate(LocalDate.of(2026, 8, 2));
//
//        ValidationException exception = expect(ValidationException.class, () -> service.createContract(dto));
//        assertEquals("error.invalidContractPeriod", exception.getMessageKey());
//    }
//
//    @Test
//    public void createContractRejectsWeeklyHoursOverEmployeeCap() {
//        ContractEntity existing = new ContractEntity();
//        existing.setStatus(ContractStatus.STARTED);
//        existing.setHoursPerWeek(15.0);
//        contractDao.byEmployee.add(existing);
//
//        BusinessRuleViolationException exception = expect(BusinessRuleViolationException.class,
//                () -> service.createContract(validDto()));
//        assertEquals("error.contractHoursCapExceeded", exception.getMessageKey());
//    }
//
//    @Test
//    public void startPreparedContractCreatesOneTimesheetPerMonth() {
//        ContractEntity contract = contract(ContractStatus.PREPARED);
//        contractDao.contract = contract;
//
//        service.startContract(41L);
//
//        assertEquals(ContractStatus.STARTED, contract.getStatus());
//        assertEquals(3, timesheetDao.saved.size());
//    }
//
//    @Test
//    public void startStartedContractUsesInvalidStateException() {
//        contractDao.contract = contract(ContractStatus.STARTED);
//
//        InvalidStateTransitionException exception = expect(InvalidStateTransitionException.class,
//                () -> service.startContract(41L));
//        assertEquals("error.contractNotPrepared", exception.getMessageKey());
//    }
//
//    @Test
//    public void terminationWithUnsavedEntriesUsesWarningException() {
//        ContractEntity contract = contract(ContractStatus.STARTED);
//        contractDao.contract = contract;
//        TimesheetEntity timesheet = new TimesheetEntity();
//        timesheet.setStatus(TimeSheetStatus.IN_PROGRESS);
//        timesheet.setContract(contract);
//        timesheet.addEntry(new sierra.tms.entities.TimesheetEntryEntity());
//        timesheetDao.byContract.add(timesheet);
//
//        TerminationWarning exception = expect(TerminationWarning.class,
//                () -> service.terminateContract(41L, false));
//        assertEquals("warning.contract.terminationWillDeleteInProgressEntries", exception.getMessageKey());
//    }
//
//    @Test
//    public void terminationWithPendingEmployeeSignatureUsesBusinessException() {
//        ContractEntity contract = contract(ContractStatus.STARTED);
//        contractDao.contract = contract;
//        TimesheetEntity timesheet = new TimesheetEntity();
//        timesheet.setStatus(TimeSheetStatus.SIGNED_BY_EMPLOYEE);
//        timesheet.setContract(contract);
//        timesheetDao.byContract.add(timesheet);
//
//        BusinessRuleViolationException exception = expect(BusinessRuleViolationException.class,
//                () -> service.terminateContract(41L, true));
//        assertEquals("error.contract.blockedByPendingSupervisorSignature", exception.getMessageKey());
//    }
//
//    private static ContractDto validDto() {
//        ContractDto dto = new ContractDto();
//        dto.setName("UI test contract");
//        dto.setEmployeeId(1L);
//        dto.setSupervisorId(2L);
//        dto.setStartDate(LocalDate.of(2026, 8, 1));
//        dto.setEndDate(LocalDate.of(2026, 10, 31));
//        dto.setFrequency(Frequency.MONTHLY);
//        dto.setHoursPerWeek(10.0);
//        dto.setWorkingDaysPerWeek(5);
//        dto.setVacationDaysPerYear(20);
//        dto.setArchiveDuration(24);
//        return dto;
//    }
//
//    private static ContractEntity contract(ContractStatus status) {
//        ContractEntity contract = new ContractEntity();
//        contract.setId(41L);
//        contract.setStatus(status);
//        contract.setStartDate(LocalDate.of(2026, 8, 1));
//        contract.setEndDate(LocalDate.of(2026, 10, 31));
//        contract.setFrequency(Frequency.MONTHLY);
//        return contract;
//    }
//
//    private static PersonEntity person(Long id, RoleType roleType) {
//        PersonEntity person = new PersonEntity();
//        person.setId(id);
//        RoleEntity role = new RoleEntity();
//        role.setRole(roleType);
//        person.addRole(role);
//        return person;
//    }
//
//    private static void inject(Object target, String name, Object value) throws Exception {
//        Field field = target.getClass().getDeclaredField(name);
//        field.setAccessible(true);
//        field.set(target, value);
//    }
//
//    private static <T extends Throwable> T expect(Class<T> type, ThrowingAction action) {
//        try { action.run(); } catch (Throwable exception) {
//            if (type.isInstance(exception)) return type.cast(exception);
//            throw new AssertionError("Expected " + type.getSimpleName(), exception);
//        }
//        throw new AssertionError("Expected " + type.getSimpleName());
//    }
//
//    private interface ThrowingAction { void run(); }
//
//    private static class InMemoryContractDao extends ContractDao {
//        private ContractEntity contract;
//        private ContractEntity saved;
//        private final List<ContractEntity> byEmployee = new ArrayList<>();
//        @Override public ContractEntity findById(Long id) { return contract != null && id.equals(contract.getId()) ? contract : null; }
//        @Override public List<ContractEntity> findByEmployee(Long employeeId) { return byEmployee; }
//        @Override public void save(ContractEntity entity) { saved = entity; }
//    }
//
//    private static class InMemoryTimesheetDao extends TimesheetDao {
//        private final List<TimesheetEntity> saved = new ArrayList<>();
//        private final List<TimesheetEntity> byContract = new ArrayList<>();
//        @Override public void save(TimesheetEntity entity) { saved.add(entity); }
//        @Override public List<TimesheetEntity> findByContractId(Long contractId) { return byContract; }
//    }
//
//    private static class InMemoryPersonDao extends PersonDao {
//        private final Map<Long, PersonEntity> people = new HashMap<>();
//        @Override public PersonEntity findById(Long id) { return people.get(id); }
//    }
//}
