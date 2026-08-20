package sierra.tms.services;

import jakarta.ejb.Local;

@Local
public interface SendReminderService {
    void sendDailyReminders();
}
