//package sierra.tms.service;
//
//import static org.junit.Assert.assertEquals;
//import java.lang.reflect.Field;
//import java.time.LocalDate;
//import org.junit.Before;
//import org.junit.Test;
//import sierra.tms.entities.ContractEntity;
//import sierra.tms.exceptions.ValidationException;
//import sierra.tms.services.HolidayService;
//import sierra.tms.services.impl.ContractHoursCalculationServiceImpl;
//import sierra.tms.utils.enums.States;
//
///** CN4a-CN4c calculation tests, including invalid-input exceptions. */
//public class ContractHoursServiceTest {
//
//    private ContractHoursCalculationServiceImpl service;
//
//    @Before
//    public void setUp() throws Exception {
//        service = new ContractHoursCalculationServiceImpl();
//        Field field = ContractHoursCalculationServiceImpl.class.getDeclaredField("holidayService");
//        field.setAccessible(true);
//        field.set(service, new OneHolidayService());
//    }
//
//    @Test
//    public void vacationHoursAreProportionalToContractMonths() {
//        assertEquals(20.0, service.calculateVacationHours(
//                contract(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), 20, 20.0, 5)), 0.0001);
//    }
//
//    @Test
//    public void hoursDueSubtractsPublicHolidays() {
//        assertEquals(16.0, service.calculateHoursDue(
//                contract(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 20, 20.0, 5),
//                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5)), 0.0001);
//    }
//
//    @Test
//    public void reportedAndRemainingHoursAreCalculated() {
//        assertEquals(7.5, service.calculateRemainingHours(20.0, 12.5), 0.0001);
//    }
//
//    @Test
//    public void incompleteContractUsesValidationException() {
//        ValidationException exception = expect(ValidationException.class,
//                () -> service.calculateVacationHours(new ContractEntity()));
//        assertEquals("error.requiredField", exception.getMessageKey());
//    }
//
//    @Test
//    public void reversedTimesheetPeriodUsesValidationException() {
//        ValidationException exception = expect(ValidationException.class,
//                () -> service.calculateHoursDue(
//                        contract(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 20, 20.0, 5),
//                        LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 1)));
//        assertEquals("error.invalidTimesheetPeriod", exception.getMessageKey());
//    }
//
//    private static ContractEntity contract(LocalDate start, LocalDate end, int vacationDays,
//            double hoursPerWeek, int workingDays) {
//        ContractEntity contract = new ContractEntity();
//        contract.setStartDate(start);
//        contract.setEndDate(end);
//        contract.setVacationDaysPerYear(vacationDays);
//        contract.setHoursPerWeek(hoursPerWeek);
//        contract.setWorkingDaysPerWeek(workingDays);
//        return contract;
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
//    private static class OneHolidayService implements HolidayService {
//        @Override public boolean isPublicHoliday(LocalDate date) { return LocalDate.of(2026, 6, 4).equals(date); }
//        @Override public boolean isPublicHoliday(LocalDate date, States state) { return isPublicHoliday(date); }
//    }
//}
