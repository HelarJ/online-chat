package ee.oop.onlinechat;

import ee.ut.oop.Message;

import java.util.HashMap;
import java.util.List;

public class ChatLogHandler {

    private HashMap<String, ChatLog> chatLogs;

    public ChatLogHandler() {
        this.chatLogs = new HashMap<>();
        getDBChannels();
    }

    public void logMessage (String channelName, Message msg){
        chatLogs.get(channelName).logMessage(msg);
    }

    public Message[] getLastMessages(String channelName, int amount){
        try {
            return chatLogs.get(channelName).getLastMessages(amount);
        } catch (NullPointerException e) { // kui sõnumeid pole veel saadetud, ei saa mingisugust infot ka tagasi saata, seega exception
            return null;
        }
    }

    private void getDBChannels(){
        SQLConnection sqlConnection = new SQLConnection();
        List<String> channelList = sqlConnection.getChannels();
        for (String channel: channelList){
            addChannel(channel);
        }

    }

    public void addChannel(String channelName){
        chatLogs.put(channelName, new ChatLog(channelName));
    }
}
