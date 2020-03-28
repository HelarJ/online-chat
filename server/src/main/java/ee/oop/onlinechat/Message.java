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
    Instant timestamp;

    public Message(String username, String message) {
        this.username = username;
        this.message = message;
        this.timestamp = java.time.Instant.now();
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
        return ("["+formatter.format(timestamp)+"] "+username+": " + message);
    }
}
