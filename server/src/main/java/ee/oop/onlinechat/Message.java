package ee.oop.onlinechat;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;


public class Message {
    private String username;
    private String message;
    private String timestamp;
    private String channelName;

    public Message(String channelName, String username, String message) {
        this.channelName = channelName;
        this.username = username;
        this.message = message;
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
        this.timestamp = formatter.format(java.time.Instant.now());
    }

    public Message(String channelName, String username, String message, String timestamp) {
        this.channelName = channelName;
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

    public String getChannelName() {return channelName;}

    @Override
    public String toString() {
        return String.format("[%s] #%s %s: %s", timestamp, channelName, username, message);
    }
}
