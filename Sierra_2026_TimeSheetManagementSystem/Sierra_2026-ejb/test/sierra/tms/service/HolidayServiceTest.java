//package sierra.tms.service;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertTrue;
//import java.lang.reflect.Field;
//import java.time.LocalDate;
//import org.junit.Before;
//import org.junit.Test;
//import sierra.tms.exceptions.ValidationException;
//import sierra.tms.services.impl.HolidayServiceImpl;
//import sierra.tms.utils.ConfigService;
//import sierra.tms.utils.enums.States;
//
///** CN4d/CN4e holiday rules and their validation exceptions. */
//public class HolidayServiceTest {
//
//    private HolidayServiceImpl holidayService;
//
//    @Before
//    public void setUp() throws Exception {
//        holidayService = new HolidayServiceImpl();
//        Field field = HolidayServiceImpl.class.getDeclaredField("configService");
//        field.setAccessible(true);
//        field.set(holidayService, new TestConfigService());
//    }
//
//    @Test
//    public void recognizesFixedAndEasterRelatedHolidaysInRhinelandPalatinate() {
//        assertTrue(holidayService.isPublicHoliday(LocalDate.of(2026, 1, 1), States.RHINELAND_PALATINATE));
//        assertTrue(holidayService.isPublicHoliday(LocalDate.of(2026, 4, 3), States.RHINELAND_PALATINATE));
//        assertTrue(holidayService.isPublicHoliday(LocalDate.of(2026, 6, 4), States.RHINELAND_PALATINATE));
//        assertFalse(holidayService.isPublicHoliday(LocalDate.of(2026, 6, 3), States.RHINELAND_PALATINATE));
//    }
//
//    @Test
//    public void supportsStateSpecificHolidays() {
//        assertTrue(holidayService.isPublicHoliday(LocalDate.of(2026, 3, 8), States.BERLIN));
//        assertFalse(holidayService.isPublicHoliday(LocalDate.of(2026, 3, 8), States.RHINELAND_PALATINATE));
//    }
//
//    @Test
//    public void missingDateUsesValidationException() {
//        ValidationException exception = expect(ValidationException.class,
//                () -> holidayService.isPublicHoliday(null, States.RHINELAND_PALATINATE));
//        assertEquals("error.requiredField", exception.getMessageKey());
//    }
//
//    @Test
//    public void missingStateUsesValidationException() {
//        ValidationException exception = expect(ValidationException.class,
//                () -> holidayService.isPublicHoliday(LocalDate.of(2026, 1, 1), null));
//        assertEquals("error.requiredField", exception.getMessageKey());
//    }
//
//    @Test
//    public void yearOutsideConfiguredRangeUsesValidationException() {
//        ValidationException exception = expect(ValidationException.class,
//                () -> holidayService.isPublicHoliday(LocalDate.of(2031, 1, 1), States.RHINELAND_PALATINATE));
//        assertEquals("error.holidayYearOutOfRange", exception.getMessageKey());
//    }
//
//    private static <T extends Throwable> T expect(Class<T> type, ThrowingAction action) {
//        try {
//            action.run();
//        } catch (Throwable exception) {
//            if (type.isInstance(exception)) {
//                return type.cast(exception);
//            }
//            throw new AssertionError("Expected " + type.getSimpleName(), exception);
//        }
//        throw new AssertionError("Expected " + type.getSimpleName());
//    }
//
//    private interface ThrowingAction { void run(); }
//
//    private static class TestConfigService extends ConfigService {
//        @Override public States getDefaultHolidayState() { return States.RHINELAND_PALATINATE; }
//        @Override public int getHolidayMinYear() { return 2025; }
//        @Override public int getHolidayMaxYear() { return 2030; }
//    }
//}
