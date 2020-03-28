package ee.oop.onlinechat;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.sql.*;

/**
 * Class for connecting to 'onlinechat' database.
 */
public class SQLConnection {
    private final String ip = "ddns.jaadla.com";
    private final String port = "3306";
    private final String andmebaas = "onlinechat";
    private final String andmebaasiKasutaja = "onlinechat_user";
    private final String andmebaasiParool = "turvalineparool"; // todo need tuleb ka encryptida, arvutist sisse lugeda?
    private final String andmebaasiUrl =  ("jdbc:mysql://"+ip+":"+port+"/"+andmebaas+"?" +
            "user="+andmebaasiKasutaja+"&password="+andmebaasiParool);

    public int register(String kasutajaNimi, String parool){
        try (Connection ühendus = DriverManager.getConnection(andmebaasiUrl)) {

            PreparedStatement stmt = ühendus.prepareStatement("CALL sp_create_user(?,?)");
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
                return 1;
            }
            return 0;
        } catch (SQLException e) {
            System.out.println("Error connecting to the database (register).");
            System.out.println(e.getErrorCode());
            System.out.println(e.getSQLState());
            System.out.println(e.getMessage());
            return 1;
        }
    }

    public int login(String kasutajaNimi, String s_parool) {
        try (Connection ühendus = DriverManager.getConnection(andmebaasiUrl)) {
            PreparedStatement stmt = ühendus.prepareStatement("CALL sp_login(?)");
            stmt.setString(1, kasutajaNimi);

            try {
                ResultSet rs = stmt.executeQuery();
                rs.next();
                String Token = rs.getString("parool");

                if (BCrypt.checkpw(s_parool, Token)) {
                    return 0;
                }
            } catch (SQLException e) {
                System.out.println("Error logging in user.");
                System.out.println(e.getErrorCode());
                System.out.println(e.getSQLState());
                System.out.println(e.getMessage());

            }
            return 1;
        } catch (SQLException e) {
            System.out.println("Error connecting to the database (login).");
            System.out.println(e.getErrorCode());
            System.out.println(e.getSQLState());
            System.out.println(e.getMessage());
            return 1;
        }
    }

    public void logMessage(String channelName, Message msg){
        try (Connection ühendus = DriverManager.getConnection(andmebaasiUrl)) {
            Statement stmt = ühendus.createStatement();
            stmt.executeQuery("CALL "+andmebaas+".sp_log_message('" + channelName + "', '" +msg.getUsername()+"', '"+msg.getMessage()+"');");

        } catch (SQLException e){
            System.out.println("Error connecting to the database (logMessage).");
            System.out.println(e.getErrorCode());
            System.out.println(e.getSQLState());
            System.out.println(e.getMessage());
        }

    }
}