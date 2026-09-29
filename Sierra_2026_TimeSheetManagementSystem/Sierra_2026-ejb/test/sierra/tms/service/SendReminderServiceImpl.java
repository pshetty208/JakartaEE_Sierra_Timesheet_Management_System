//package sierra.tms.service;
//
//import jakarta.annotation.Resource;
//import jakarta.annotation.security.PermitAll;
//import jakarta.ejb.EJB;
//import jakarta.ejb.Schedule;
//import jakarta.ejb.Stateless;
//import jakarta.mail.Session;
//import jakarta.mail.Message;
//import jakarta.mail.MessagingException;
//import jakarta.mail.Transport;
//import jakarta.mail.internet.InternetAddress;
//import jakarta.mail.internet.MimeMessage;
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import sierra.tms.dao.TimesheetDao;
//import sierra.tms.entities.ContractEntity;
//import sierra.tms.entities.PersonEntity;
//import sierra.tms.entities.TimesheetEntity;
//import sierra.tms.services.ReminderService;
//import sierra.tms.utils.enums.TimeSheetStatus;
//
//@Stateless
//public class SendReminderServiceImpl implements ReminderService {
//
//    @EJB
//    private TimesheetDao timesheetDao;
//
//    @Resource(mappedName = "mail/tssMailSession")
//    private Session mailSession;
//
//    @Override
//    @PermitAll
//
//    // TESTING ONLY:
//    // Runs every minute instead of every day at 07:00.
//    @Schedule(
//            hour = "*",
//            minute = "*",
//            second = "0",
//            persistent = false
//    )
//    public void sendDailyReminders() {
//
//        System.out.println("======================================");
//        System.out.println("DAILY REMINDER JOB STARTED");
//        System.out.println("Today: " + LocalDate.now());
//        System.out.println("======================================");
//
//        Map<String, List<String>> remindersByEmail =
//                new LinkedHashMap<>();
//
//        LocalDate today = LocalDate.now();
//
//        collectEmployeeReminders(
//                remindersByEmail,
//                today
//        );
//
//        collectSupervisorAndAssistantReminders(
//                remindersByEmail
//        );
//
//        collectSecretaryReminders(
//                remindersByEmail
//        );
//
//        System.out.println(
//                "Number of recipients: "
//                + remindersByEmail.size()
//        );
//
//        if (remindersByEmail.isEmpty()) {
//            System.out.println(
//                    "No reminders need to be sent."
//            );
//        }
//
//        for (Map.Entry<String, List<String>> entry
//                : remindersByEmail.entrySet()) {
//
//            System.out.println("--------------------------------------");
//            System.out.println(
//                    "Sending reminder to: "
//                    + entry.getKey()
//            );
//
//            System.out.println(
//                    "Number of messages: "
//                    + entry.getValue().size()
//            );
//
//            for (String message : entry.getValue()) {
//                System.out.println(
//                        "Message: " + message
//                );
//            }
//
//            sendEmail(
//                    entry.getKey(),
//                    entry.getValue()
//            );
//        }
//
//        System.out.println("======================================");
//        System.out.println("DAILY REMINDER JOB FINISHED");
//        System.out.println("======================================");
//    }
//
//    private void addReminder(
//            Map<String, List<String>> remindersByEmail,
//            PersonEntity person,
//            String message) {
//
//        if (person == null) {
//            System.out.println(
//                    "Reminder skipped: person is null."
//            );
//            return;
//        }
//
//        String email = person.getEmailAddress();
//
//        if (email == null || email.isBlank()) {
//            System.out.println(
//                    "Reminder skipped: person has no email address."
//            );
//            return;
//        }
//
//        remindersByEmail
//                .computeIfAbsent(
//                        email,
//                        key -> new ArrayList<>()
//                )
//                .add(message);
//
//        System.out.println(
//                "Reminder added for: " + email
//        );
//    }
//
//    private void sendEmail(
//            String toAddress,
//            List<String> messages) {
//
//        System.out.println(
//                "Attempting to send email to: "
//                + toAddress
//        );
//
//        try {
//
//            if (mailSession == null) {
//                System.err.println(
//                        "ERROR: mailSession is NULL."
//                );
//                System.err.println(
//                        "Check GlassFish JavaMail resource:"
//                );
//                System.err.println(
//                        "mail/tssMailSession"
//                );
//                return;
//            }
//
//            MimeMessage mimeMessage =
//                    new MimeMessage(mailSession);
//
//            mimeMessage.setFrom(
//                    new InternetAddress(
//                            "prajnashetty73@uni-koblenz.de"
//                    )
//            );
//
//            mimeMessage.setRecipient(
//                    Message.RecipientType.TO,
//                    new InternetAddress(toAddress)
//            );
//
//            mimeMessage.setSubject(
//                    "TSS: You have "
//                    + messages.size()
//                    + " pending reminder(s)"
//            );
//
//            mimeMessage.setText(
//                    String.join(
//                            "\n\n",
//                            messages
//                    )
//            );
//
//            Transport.send(mimeMessage);
//
//            System.out.println(
//                    "EMAIL SENT SUCCESSFULLY TO: "
//                    + toAddress
//            );
//
//        } catch (MessagingException e) {
//
//            System.err.println(
//                    "======================================"
//            );
//
//            System.err.println(
//                    "EMAIL SENDING FAILED"
//            );
//
//            System.err.println(
//                    "Recipient: " + toAddress
//            );
//
//            System.err.println(
//                    "Error: " + e.getMessage()
//            );
//
//            e.printStackTrace();
//
//            System.err.println(
//                    "======================================"
//            );
//        }
//    }
//
//    private void collectEmployeeReminders(
//            Map<String, List<String>> remindersByEmail,
//            LocalDate today) {
//
//        System.out.println(
//                "Checking employee reminders..."
//        );
//
//        List<TimesheetEntity> timesheets =
//                timesheetDao.findByStatus(
//                        TimeSheetStatus.IN_PROGRESS
//                );
//
//        System.out.println(
//                "IN_PROGRESS timesheets found: "
//                + timesheets.size()
//        );
//
//        for (TimesheetEntity t : timesheets) {
//
//            System.out.println(
//                    "Checking timesheet: "
//                    + t.getStartDate()
//                    + " -> "
//                    + t.getEndDate()
//            );
//
//            if (t.getEndDate() == null) {
//
//                System.out.println(
//                        "Skipped: end date is null."
//                );
//
//                continue;
//            }
//
//            if (today.isBefore(t.getEndDate())) {
//
//                System.out.println(
//                        "Skipped: timesheet period has not ended."
//                );
//
//                continue;
//            }
//
//            ContractEntity contract =
//                    t.getContract();
//
//            if (contract == null) {
//
//                System.out.println(
//                        "Skipped: contract is null."
//                );
//
//                continue;
//            }
//
//            PersonEntity employee =
//                    contract.getEmployee();
//
//            if (employee == null) {
//
//                System.out.println(
//                        "Skipped: employee is null."
//                );
//
//                continue;
//            }
//
//            System.out.println(
//                    "Employee reminder required for: "
//                    + employee.getEmailAddress()
//            );
//
//            String message =
//                    "Your timesheet for "
//                    + t.getStartDate()
//                    + " to "
//                    + t.getEndDate()
//                    + " is still incomplete. "
//                    + "Please fill it in.";
//
//            addReminder(
//                    remindersByEmail,
//                    employee,
//                    message
//            );
//        }
//    }
//
//    private void collectSupervisorAndAssistantReminders(
//            Map<String, List<String>> remindersByEmail) {
//
//        System.out.println(
//                "Checking supervisor and assistant reminders..."
//        );
//
//        List<TimesheetEntity> timesheets =
//                timesheetDao.findByStatus(
//                        TimeSheetStatus.SIGNED_BY_EMPLOYEE
//                );
//
//        System.out.println(
//                "SIGNED_BY_EMPLOYEE timesheets found: "
//                + timesheets.size()
//        );
//
//        for (TimesheetEntity t : timesheets) {
//
//            ContractEntity contract =
//                    t.getContract();
//
//            if (contract == null) {
//
//                System.out.println(
//                        "Skipped: contract is null."
//                );
//
//                continue;
//            }
//
//            String message =
//                    "A timesheet ("
//                    + t.getStartDate()
//                    + " to "
//                    + t.getEndDate()
//                    + ") is signed by the employee "
//                    + "and awaiting your review.";
//
//            PersonEntity supervisor =
//                    contract.getSupervisor();
//
//            addReminder(
//                    remindersByEmail,
//                    supervisor,
//                    message
//            );
//
//            if (contract.getAssistants() != null) {
//
//                for (PersonEntity assistant
//                        : contract.getAssistants()) {
//
//                    addReminder(
//                            remindersByEmail,
//                            assistant,
//                            message
//                    );
//                }
//
//            } else {
//
//                System.out.println(
//                        "Contract has no assistants."
//                );
//            }
//        }
//    }
//
//    private void collectSecretaryReminders(
//            Map<String, List<String>> remindersByEmail) {
//
//        System.out.println(
//                "Checking secretary reminders..."
//        );
//
//        List<TimesheetEntity> timesheets =
//                timesheetDao.findByStatus(
//                        TimeSheetStatus.SIGNED_BY_SUPERVISOR
//                );
//
//        System.out.println(
//                "SIGNED_BY_SUPERVISOR timesheets found: "
//                + timesheets.size()
//        );
//
//        for (TimesheetEntity t : timesheets) {
//
//            ContractEntity contract =
//                    t.getContract();
//
//            if (contract == null) {
//
//                System.out.println(
//                        "Skipped: contract is null."
//                );
//
//                continue;
//            }
//
//            String message =
//                    "A timesheet ("
//                    + t.getStartDate()
//                    + " to "
//                    + t.getEndDate()
//                    + ") is fully signed "
//                    + "and ready to be archived.";
//
//            if (contract.getSecretaries() != null) {
//
//                for (PersonEntity secretary
//                        : contract.getSecretaries()) {
//
//                    addReminder(
//                            remindersByEmail,
//                            secretary,
//                            message
//                    );
//                }
//
//            } else {
//
//                System.out.println(
//                        "Contract has no secretaries."
//                );
//            }
//        }
//    }
//}