package ee.oop.onlinechat;

public class ClientInfo {
    String name;
    Boolean loggedIn;

    public ClientInfo() {
        this.name = "Default";
        this.loggedIn = false;
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
}
