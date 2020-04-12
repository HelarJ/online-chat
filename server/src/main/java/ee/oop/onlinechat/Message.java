package ee.oop.onlinechat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Locale;

public class Message {
    String username;
    String message;
    String timestamp;

    public Message(String username, String message) {
        this.username = username;
        this.message = message;
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
        this.timestamp = formatter.format(java.time.Instant.now());
    }

    public Message(String username, String message, String timestamp) {
        this.username = username;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return ("["+(timestamp)+"] "+username+": " + message);
    }
}
