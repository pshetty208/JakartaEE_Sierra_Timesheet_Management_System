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

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import sierra.tms.dao.TimesheetDao;
import sierra.tms.entities.ContractEntity;
import sierra.tms.entities.PersonEntity;
import sierra.tms.entities.TimesheetEntity;
import sierra.tms.services.ReminderService;
import sierra.tms.utils.enums.TimeSheetStatus;

import static sierra.tms.config.ApplicationConfig.TIME_ZONE;
import static sierra.tms.config.ApplicationConfig.TIME_ZONE_ID;

@Stateless
public class ReminderServiceImpl implements ReminderService {

    private static final String SENDER_EMAIL =
            "noreply@sierra-tss.example";
    private static final String EMAIL_SUBJECT =
            "TSS: You have pending timesheet reminders";

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
        Map<String, Set<String>> remindersByRecipient =
                collectDailyReminders(today);

        remindersByRecipient.forEach(this::sendEmail);
    }

    /** RE5: collect every reminder before sending one email per address. */
    private Map<String, Set<String>> collectDailyReminders(LocalDate today) {
        Map<String, Set<String>> remindersByRecipient =
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
            Map<String, Set<String>> remindersByRecipient,
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
                    createEmployeeMessage(timesheet));
        }
    }

    /**
     * RE2: remind the supervisor and assistants when an employee has signed
     * a timesheet and it is waiting for their review.
     */
    private void collectReviewerReminders(
            Map<String, Set<String>> remindersByRecipient) {
        for (TimesheetEntity timesheet : timesheetDao.findByStatus(
                TimeSheetStatus.SIGNED_BY_EMPLOYEE)) {
            ContractEntity contract = timesheet.getContract();
            String message = createApprovalMessage(timesheet);

            addContractReviewers(
                    remindersByRecipient,
                    contract,
                    message);
        }
    }

    /**
     * RE3: remind secretaries when a supervisor has signed a timesheet and it
     * is waiting for their processing.
     */
    private void collectSecretaryReminders(
            Map<String, Set<String>> remindersByRecipient) {
        for (TimesheetEntity timesheet : timesheetDao.findByStatus(
                TimeSheetStatus.SIGNED_BY_SUPERVISOR)) {
            String message = createSecretaryMessage(timesheet);

            for (PersonEntity secretary
                    : timesheet.getContract().getSecretaries()) {
                addReminder(remindersByRecipient, secretary, message);
            }
        }
    }

    private void addContractReviewers(
            Map<String, Set<String>> remindersByRecipient,
            ContractEntity contract,
            String message) {
        addReminder(
                remindersByRecipient,
                contract.getSupervisor(),
                message);

        for (PersonEntity assistant : contract.getAssistants()) {
            addReminder(remindersByRecipient, assistant, message);
        }
    }

    private boolean isReminderDue(
            TimesheetEntity timesheet,
            LocalDate today) {
        return timesheet.getEndDate() != null
                && !today.isBefore(timesheet.getEndDate());
    }

    private void addReminder(
            Map<String, Set<String>> remindersByRecipient,
            PersonEntity recipient,
            String message) {
        if (recipient == null || recipient.getEmailAddress() == null
                || recipient.getEmailAddress().isBlank()) {
            return;
        }

        // Normalization prevents different casing or spaces from creating
        // multiple emails for the same address.
        String email = recipient.getEmailAddress()
                .trim()
                .toLowerCase(Locale.ROOT);
        remindersByRecipient
                .computeIfAbsent(email, ignored -> new LinkedHashSet<>())
                .add(message);
    }

    private String createEmployeeMessage(TimesheetEntity timesheet) {
        return "Your timesheet from " + timesheet.getStartDate()
                + " to " + timesheet.getEndDate()
                + " is still in progress. Please complete and sign it.";
    }

    private String createApprovalMessage(TimesheetEntity timesheet) {
        PersonEntity employee = timesheet.getContract().getEmployee();

        return "The timesheet for " + employee.getFirstName()
                + " " + employee.getLastName()
                + " from " + timesheet.getStartDate()
                + " to " + timesheet.getEndDate()
                + " has been signed by the employee."
                + " Please review, sign, or reject it.";
    }

    private String createSecretaryMessage(TimesheetEntity timesheet) {
        PersonEntity employee = timesheet.getContract().getEmployee();

        return "The timesheet for " + employee.getFirstName()
                + " " + employee.getLastName()
                + " from " + timesheet.getStartDate()
                + " to " + timesheet.getEndDate()
                + " has been signed by the supervisor."
                + " Please process it.";
    }

    private void sendEmail(String recipient, Set<String> reminders) {
        try {
            MimeMessage email = new MimeMessage(mailSession);
            email.setFrom(new InternetAddress(SENDER_EMAIL));
            email.setRecipient(
                    Message.RecipientType.TO,
                    new InternetAddress(recipient));
            email.setSubject(EMAIL_SUBJECT, "UTF-8");
            email.setText(String.join("\n\n", reminders), "UTF-8");
            Transport.send(email);
        } catch (MessagingException exception) {
            LOGGER.log(
                    Level.SEVERE,
                    "Could not send timesheet reminder to " + recipient,
                    exception);
        }
    }
}
