package ee.oop.onlinechat;

import java.util.HashSet;
import java.util.Set;

public class ClientInfo {
    private String name;
    private Boolean loggedIn;
    private Set<String> joinedChannels;
    private ClientType clientType;

    public ClientInfo() {
        this.name = "Default";
        this.loggedIn = false;
        this.joinedChannels = new HashSet<>();
        this.clientType = null;
    }

    public void setClientType(ClientType clientType){
        this.clientType = clientType;
    }
    public ClientType getClientType(){
        return clientType;
    }
    public boolean isWs(){
        return clientType == ClientType.WEBSOCKET;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(Boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public void joinChannel(String channel){
        joinedChannels.add(channel);
    }

    public void leaveChannel(String channel){
        joinedChannels.remove(channel);
    }

    public boolean isInChannel(String channel){
        return joinedChannels.contains(channel);
    }
}
