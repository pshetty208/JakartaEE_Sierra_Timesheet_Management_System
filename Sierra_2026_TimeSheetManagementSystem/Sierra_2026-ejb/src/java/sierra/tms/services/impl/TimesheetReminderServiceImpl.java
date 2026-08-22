//package sierra.tms.services.impl;
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
//import java.time.LocalDate;
//import jakarta.mail.internet.MimeMessage;
//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import static sierra.tms.config.ApplicationConfig.TIME_ZONE_ID;
//import sierra.tms.dao.TimesheetDao;
//import sierra.tms.entities.ContractEntity;
//import sierra.tms.entities.PersonEntity;
//import sierra.tms.entities.TimesheetEntity;
//import sierra.tms.services.ReminderService;
//import sierra.tms.utils.enums.TimeSheetStatus;
//
//@Stateless
//public class ReminderServiceImpl implements ReminderService{
//    
//    private static final Logger LOGGER = Logger.getLogger(TimesheetReminderServiceImpl.class.getName());
//
//    @EJB
//    private TimesheetDao timesheetDao;
//
//    @Resource(mappedName = "mail/tssMailSession")
//    private Session mailSession;
//    
//    @Override
//    @PermitAll
//    @Schedule(hour = "7", minute = "0", second = "0", timezone = TIME_ZONE_ID, persistent = true)
//    public void sendDailyReminders() {
//        Map<String, List<String>> remindersByEmail = new LinkedHashMap<>();
//        LocalDate today = LocalDate.now();
//
//        collectEmployeeReminders(remindersByEmail, today);          
//        collectSupervisorAndAssistantReminders(remindersByEmail);   
//        collectSecretaryReminders(remindersByEmail);                
//
//        for (Map.Entry<String, List<String>> entry : remindersByEmail.entrySet()) {
//            sendEmail(entry.getKey(), entry.getValue());
//        }
//    }
//
//    private void addReminder(Map<String, List<String>> remindersByEmail, PersonEntity person, String message) {
//        if (person == null || person.getEmailAddress() == null) {
//            return;
//        }
//        remindersByEmail.computeIfAbsent(person.getEmailAddress(), k -> new ArrayList<>()).add(message);
//    }
//
//    private void sendEmail(String toAddress, List<String> messages) {
//        try {
//            MimeMessage mimeMessage = new MimeMessage(mailSession);
//            mimeMessage.setFrom(new InternetAddress("noreply@sierra-tss.example"));
//            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
//            mimeMessage.setSubject("TSS: You have " + messages.size() + " reminder(s)");
//            mimeMessage.setText(String.join("\n\n", messages), "UTF-8");
//            Transport.send(mimeMessage);
//        } catch (MessagingException e) {
//            LOGGER.log(Level.SEVERE, "Could not send timesheet reminder to " + recipient, exception);
//        }
//    }
//
//    private void collectEmployeeReminders(Map<String, List<String>> remindersByEmail, LocalDate today) {
//        for (TimesheetEntity t : timesheetDao.findByStatus(TimeSheetStatus.IN_PROGRESS)) {
//            if (t.getEndDate() == null || today.isBefore(t.getEndDate())) {
//                continue; // not yet at the last day of the period
//            }
//            ContractEntity contract = t.getContract();
//            PersonEntity employee = contract == null ? null : contract.getEmployee();
//            addReminder(remindersByEmail, employee,
//                    "Your timesheet for " + t.getStartDate() + " to " + t.getEndDate()
//                            + " is still incomplete. Please fill it in.");
//        }
//    }
//    
//    private void collectSupervisorAndAssistantReminders(Map<String, List<String>> remindersByEmail) {
//        for (TimesheetEntity t : timesheetDao.findByStatus(TimeSheetStatus.SIGNED_BY_EMPLOYEE)) {
//            ContractEntity contract = t.getContract();
//            if (contract == null) {
//                continue;
//            }
//            String message = "A timesheet (" + t.getStartDate() + " to " + t.getEndDate()
//                    + ") is signed by the employee and awaiting your review.";
//            addReminder(remindersByEmail, contract.getSupervisor(), message);
//            for (PersonEntity assistant : contract.getAssistants()) {
//                addReminder(remindersByEmail, assistant, message);
//            }
//        }
//    }
//    
//    private void collectSecretaryReminders(Map<String, List<String>> remindersByEmail) {
//        for (TimesheetEntity t : timesheetDao.findByStatus(TimeSheetStatus.SIGNED_BY_SUPERVISOR)) {
//            ContractEntity contract = t.getContract();
//            if (contract == null) {
//                continue;
//            }
//            String message = "A timesheet (" + t.getStartDate() + " to " + t.getEndDate()
//                    + ") is fully signed and ready to be archived.";
//            for (PersonEntity secretary : contract.getSecretaries()) {
//                addReminder(remindersByEmail, secretary, message);
//            }
//        }
//    }
//    
//}
