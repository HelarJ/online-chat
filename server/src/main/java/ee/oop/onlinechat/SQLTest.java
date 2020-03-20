package ee.oop.onlinechat;

import java.sql.*;

public class SQLTest {
    public static void main(String[] args) {
        try (Connection myConn = DriverManager.getConnection("jdbc:mysql://localhost:3306/studentdb", "kasutaja", "paroolike")) {
            Statement myStmt = myConn.createStatement();
            ResultSet myRs = myStmt.executeQuery("SELECT * FROM class101");

            while (myRs.next()) {
                System.out.println(myRs.getString("name") + "'s GPA: " + myRs.getString("gpa"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
