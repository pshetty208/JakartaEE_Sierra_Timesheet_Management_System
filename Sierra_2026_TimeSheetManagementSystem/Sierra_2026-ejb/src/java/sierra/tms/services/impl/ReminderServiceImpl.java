package sierra.tms.services.impl;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import sierra.tms.dao.TimesheetDao;
import sierra.tms.entities.ContractEntity;
import sierra.tms.entities.PersonEntity;
import sierra.tms.entities.TimesheetEntity;
import sierra.tms.i18n.LanguageResolver;
import sierra.tms.services.ReminderService;
import sierra.tms.utils.enums.TimeSheetStatus;

import static sierra.tms.config.ApplicationConfig.TIME_ZONE;
import static sierra.tms.config.ApplicationConfig.TIME_ZONE_ID;

@Stateless
public class ReminderServiceImpl implements ReminderService {

    private static final String SENDER_EMAIL =
            "noreply@sierra-tss.example";
    private static final String MESSAGE_BUNDLE =
            "sierra.tms.i18n.reminder_messages";

    private static final Logger LOGGER =
            Logger.getLogger(ReminderServiceImpl.class.getName());

    @EJB
    private TimesheetDao timesheetDao;

    @Resource(lookup = "mail/tssMailSession")
    private Session mailSession;

    /** RE4: GlassFish runs this job daily until a timesheet changes state. */
    @Override
    @Schedule(
            hour = "7",
            minute = "0",
            second = "0",
            timezone = TIME_ZONE_ID,
            persistent = false)
    public void sendDailyReminders() {
        LocalDate today = LocalDate.now(TIME_ZONE);
        Map<String, ReminderContent> remindersByRecipient =
                collectDailyReminders(today);

        remindersByRecipient.forEach(this::sendEmail);
    }

    /** RE5: collect every reminder before sending one email per address. */
    private Map<String, ReminderContent> collectDailyReminders(
            LocalDate today) {
        Map<String, ReminderContent> remindersByRecipient =
                new LinkedHashMap<>();

        collectInProgressTimesheetReminders(remindersByRecipient, today);
        collectReviewerReminders(remindersByRecipient);
        collectSecretaryReminders(remindersByRecipient);

        return remindersByRecipient;
    }

    /**
     * RE1: remind employees about in-progress timesheets which are due or
     * overdue. Including overdue timesheets allows reminders to repeat daily.
     */
    private void collectInProgressTimesheetReminders(
            Map<String, ReminderContent> remindersByRecipient,
            LocalDate today) {
        for (TimesheetEntity timesheet
                : timesheetDao.findByStatus(TimeSheetStatus.IN_PROGRESS)) {
            if (!isReminderDue(timesheet, today)) {
                continue;
            }

            PersonEntity employee = timesheet.getContract().getEmployee();
            addReminder(
                    remindersByRecipient,
                    employee,
                    "reminder.employee",
                    timesheet.getStartDate(),
                    timesheet.getEndDate());
        }
    }

    /**
     * RE2: remind the supervisor and assistants when an employee has signed
     * a timesheet and it is waiting for their review.
     */
    private void collectReviewerReminders(
            Map<String, ReminderContent> remindersByRecipient) {
        for (TimesheetEntity timesheet : timesheetDao.findByStatus(
                TimeSheetStatus.SIGNED_BY_EMPLOYEE)) {
            ContractEntity contract = timesheet.getContract();

            addContractReviewers(
                    remindersByRecipient,
                    contract,
                    timesheet);
        }
    }

    /**
     * RE3: remind secretaries when a supervisor has signed a timesheet and it
     * is waiting for their processing.
     */
    private void collectSecretaryReminders(
            Map<String, ReminderContent> remindersByRecipient) {
        for (TimesheetEntity timesheet : timesheetDao.findByStatus(
                TimeSheetStatus.SIGNED_BY_SUPERVISOR)) {
            PersonEntity employee = timesheet.getContract().getEmployee();

            for (PersonEntity secretary
                    : timesheet.getContract().getSecretaries()) {
                addReminder(
                        remindersByRecipient,
                        secretary,
                        "reminder.secretary",
                        employee.getFirstName() + " " + employee.getLastName(),
                        timesheet.getStartDate(),
                        timesheet.getEndDate());
            }
        }
    }

    private void addContractReviewers(
            Map<String, ReminderContent> remindersByRecipient,
            ContractEntity contract,
            TimesheetEntity timesheet) {
        PersonEntity employee = contract.getEmployee();

        addReminder(
                remindersByRecipient,
                contract.getSupervisor(),
                "reminder.reviewer",
                employee.getFirstName() + " " + employee.getLastName(),
                timesheet.getStartDate(),
                timesheet.getEndDate());

        for (PersonEntity assistant : contract.getAssistants()) {
            addReminder(
                    remindersByRecipient,
                    assistant,
                    "reminder.reviewer",
                    employee.getFirstName() + " " + employee.getLastName(),
                    timesheet.getStartDate(),
                    timesheet.getEndDate());
        }
    }

    private boolean isReminderDue(
            TimesheetEntity timesheet,
            LocalDate today) {
        return timesheet.getEndDate() != null
                && !today.isBefore(timesheet.getEndDate());
    }

    private void addReminder(
            Map<String, ReminderContent> remindersByRecipient,
            PersonEntity recipient,
            String messageKey,
            Object... messageArguments) {
        if (recipient == null || recipient.getEmailAddress() == null
                || recipient.getEmailAddress().isBlank()) {
            return;
        }

        // Normalization prevents different casing or spaces from creating
        // multiple emails for the same address.
        String email = recipient.getEmailAddress()
                .trim()
                .toLowerCase(Locale.ROOT);
        Locale locale = LanguageResolver.resolveLocale(
                recipient.getPreferredLanguage());
        ReminderContent reminderContent = remindersByRecipient
                .computeIfAbsent(
                        email,
                        ignored -> new ReminderContent(locale));

        ResourceBundle bundle = ResourceBundle.getBundle(
                MESSAGE_BUNDLE,
                reminderContent.locale);
        String message = new MessageFormat(
                bundle.getString(messageKey),
                reminderContent.locale)
                .format(localizeDates(
                        messageArguments,
                        reminderContent.locale));

        reminderContent.messages.add(message);
    }

    /**
     * Formats dates using the recipient's convention, for example
     * "Aug 26, 2026" in English and "26.08.2026" in German.
     */
    private Object[] localizeDates(Object[] arguments, Locale locale) {
        DateTimeFormatter dateFormatter = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale);
        Object[] localizedArguments = arguments.clone();

        for (int index = 0; index < localizedArguments.length; index++) {
            if (localizedArguments[index] instanceof LocalDate date) {
                localizedArguments[index] = dateFormatter.format(date);
            }
        }

        return localizedArguments;
    }

    private void sendEmail(
            String recipient,
            ReminderContent reminderContent) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(
                    MESSAGE_BUNDLE,
                    reminderContent.locale);
            MimeMessage email = new MimeMessage(mailSession);
            email.setFrom(new InternetAddress(SENDER_EMAIL));
            email.setRecipient(
                    Message.RecipientType.TO,
                    new InternetAddress(recipient));
            email.setSubject(bundle.getString("email.subject"), "UTF-8");
            email.setText(
                    String.join("\n\n", reminderContent.messages),
                    "UTF-8");
            Transport.send(email);
        } catch (MessagingException exception) {
            LOGGER.log(
                    Level.SEVERE,
                    "Could not send timesheet reminder to " + recipient,
                    exception);
        }
    }

    private static final class ReminderContent {

        private final Locale locale;
        private final Set<String> messages = new LinkedHashSet<>();

        private ReminderContent(Locale locale) {
            this.locale = locale;
        }
    }
}
