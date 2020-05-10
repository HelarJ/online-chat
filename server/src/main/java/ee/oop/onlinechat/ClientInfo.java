package ee.oop.onlinechat;

import ee.ut.oop.Crypto;

import java.util.HashSet;
import java.util.Set;

public class ClientInfo {
    private String name;
    private Boolean loggedIn;
    private Set<String> joinedChannels;
    private ClientType clientType;
    private Crypto encrypter;
    private Crypto decrypter;
    private boolean initialConnection;

    public ClientInfo() {
        this.name = "Default";
        this.loggedIn = false;
        this.joinedChannels = new HashSet<>();
        this.clientType = null;
        this.initialConnection = true;
    }
    public boolean isInitialConnection(){
        return initialConnection;
    }

    public void setClientType(ClientType clientType){
        this.initialConnection = false;
        this.clientType = clientType;
    }

    public boolean isWs(){
        return clientType == ClientType.WEBSOCKET;
    }
    public String getName() {
        return name;
    }

    public void setEncrypter(Crypto encrypter){
        this.encrypter = encrypter;
    }

    public Crypto getEncrypter(){
        return encrypter;
    }

    public void setDecrypter(Crypto decrypter){
        this.decrypter = decrypter;
    }

    public Crypto getDecrypter(){
        return decrypter;
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
