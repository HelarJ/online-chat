package ee.oop.onlinechat;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class EmailErrorHandler extends Handler {
    private Mailer mailer;
    private String adminEmail;

    public EmailErrorHandler(String adminEmail) {
        mailer = new Mailer();
        this.adminEmail = adminEmail;

    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)){
            return;
        }
        String msg = getFormatter().format(record);
        mailer.sendEmail("SEVERE ERROR: " + record.getMessage(), msg, adminEmail);

    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}
