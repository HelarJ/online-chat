package ee.oop.onlinechat;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Class for connecting to 'onlinechat' database.
 */
public class SQLConnection {
    private String credentials;

    public SQLConnection() {
        try (InputStream input = SQLConnection.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties prop = new Properties();
            assert input != null;
            prop.load(input);

            this.credentials = ("jdbc:mysql://" + prop.get("db.ip") + ":" + prop.get("db.port") + "/" + prop.get("db.name") + "?" +
                    "user=" + prop.get("db.username") + "&password=" + prop.get("db.password"));
        } catch (FileNotFoundException e) {
            System.out.println("Config file not found for database.");
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Error reading properties from config file");
            System.out.println(e.getMessage());
        }


    }

    public SQLResponse register(String kasutajaNimi, String parool){
        try (Connection ühendus = DriverManager.getConnection(credentials);
                PreparedStatement stmt = ühendus.prepareStatement("CALL sp_create_user(?,?)")) {
            stmt.setString(1, kasutajaNimi);

            try {
                String turvalineParool = BCrypt.hashpw(parool, BCrypt.gensalt());
                stmt.setString(2, turvalineParool);
                stmt.executeQuery();
            } catch (SQLException e) {
                System.out.println("Error registering for an account.");
                System.out.println(e.getErrorCode());
                System.out.println(e.getSQLState());
                System.out.println(e.getMessage());
                if (e.getErrorCode() == 1062){ //1062 is a duplicate entry errorcode.. ehk kui kasutaja on juba andmebaasis olemas
                    return SQLResponse.USEREXISTS;
                }
                return SQLResponse.ERROR;
            }
            return SQLResponse.SUCCESS;
        } catch (SQLException e) {
            System.out.println("Error connecting to the database (register).");
            System.out.println(e.getErrorCode());
            System.out.println(e.getSQLState());
            System.out.println(e.getMessage());
            return SQLResponse.ERROR;
        }
    }

    public SQLResponse login(String kasutajaNimi, String s_parool) {
        try (Connection ühendus = DriverManager.getConnection(credentials);
                PreparedStatement stmt = ühendus.prepareStatement("CALL sp_login(?)")) {
            stmt.setString(1, kasutajaNimi);

            try {
                ResultSet rs = stmt.executeQuery();
                rs.next();
                String Token = rs.getString("parool");

                if (BCrypt.checkpw(s_parool, Token)) {
                    return SQLResponse.SUCCESS;
                } else {
                    return SQLResponse.WRONGPASSWORD;
                }
            } catch (SQLException e) {
                System.out.println("Error logging in user.");
                System.out.println(e.getErrorCode());
                System.out.println(e.getSQLState());
                System.out.println(e.getMessage());
                return SQLResponse.ERROR;

            }
        } catch (SQLException e) {
            System.out.println("Error connecting to the database (login).");
            System.out.println(e.getErrorCode());
            System.out.println(e.getSQLState());
            System.out.println(e.getMessage());
            return SQLResponse.ERROR;
        }
    }

    public void logMessage(String channelName, Message msg){
        try (Connection ühendus = DriverManager.getConnection(credentials);
                PreparedStatement stmt = ühendus.prepareStatement("CALL sp_log_message(?,?,?)")) {
            stmt.setString(1, channelName);
            stmt.setString(2, msg.getUsername());
            stmt.setString(3, msg.getMessage());
            stmt.executeUpdate();

        } catch (SQLException e){
            System.out.println("Error connecting to the database (logMessage).");
            System.out.println(e.getErrorCode());
            System.out.println(e.getSQLState());
            System.out.println(e.getMessage());
        }
    }

    public ArrayList<Message> getMessages(int amount, String channelName) {
        ArrayList<Message> messageList = new ArrayList<>();
        try (Connection ühendus = DriverManager.getConnection(credentials);
                PreparedStatement stmt = ühendus.prepareStatement("CALL sp_get_n_recent_messages(?, ?)")) {
            stmt.setInt(1, amount);
            stmt.setString(2, channelName);
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                Message message = new Message(resultSet.getString("channel"),
                        resultSet.getString("username"),
                        resultSet.getString("message"),
                        resultSet.getString("timestamp"));
                messageList.add(message);
            }

        } catch (SQLException e){
            System.out.println("Error connecting to the database (getMessage).");
            System.out.println(e.getErrorCode());
            System.out.println(e.getSQLState());
            System.out.println(e.getMessage());
        }
        return messageList;
    }
}