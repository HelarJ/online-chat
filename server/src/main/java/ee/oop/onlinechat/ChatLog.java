package ee.oop.onlinechat;

import java.util.ArrayList;
import java.util.List;

public class ChatLog {
    private List<Message> logList;
    private SQLConnection sqlConnection;
    private String channelName;

    public ChatLog(String channelName) {
        this.channelName = channelName;
        this.logList = new ArrayList<>();
        this.sqlConnection = new SQLConnection();
        logList = sqlConnection.getMessages(100, this.channelName);
        System.out.println(". Loaded "+logList.size()+" messages from db.");
    }
    public String getChannelName(){
        return channelName;
    }

    public void logMessage(Message message){
        sqlConnection.logMessage(channelName, message);
    }

    public Message[] getLastMessages(int amount){
        Message[] logArray = logList.toArray(new Message[0]);
        if (logArray.length <= amount){
            return logArray;
        }
        Message[] newArray = new Message[amount];
        System.arraycopy(logArray,logArray.length-amount,newArray,0,newArray.length);
        return newArray;
    }
}
