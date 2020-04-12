package ee.oop.onlinechat;

import java.sql.*;

public class SQLTest {
    public static void main(String[] args) {
        try (Connection myConn = DriverManager.getConnection("jdbc:mysql://ddns.jaadla.com:3306/onlinechat", "onlinechat_user", "turvalineparool")) {
            Statement myStmt = myConn.createStatement();
            ResultSet myRs = myStmt.executeQuery("CALL sp_get_n_recent_messages(2)");

            while (myRs.next()) {
                System.out.println("id: " + myRs.getString("id") + ", timestamp: " + myRs.getString("timestamp") +
                        ", channel: " + myRs.getString("channel") + ", username: " + myRs.getString("username") +
                        ", message: " + myRs.getString("message"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
