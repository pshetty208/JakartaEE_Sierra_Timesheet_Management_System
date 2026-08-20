package sierra.tms.dto;

import java.io.Serializable;

public class ReminderMailDto implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final String recipientEmail;
    private final String message;

    public ReminderMailDto(String recipientEmail, String message) {
        this.recipientEmail = recipientEmail;
        this.message = message;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getMessage() {
        return message;
    }
    
}
