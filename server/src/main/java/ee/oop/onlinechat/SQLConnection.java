package ee.oop.onlinechat;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.sql.*;

/**
 * Class for connecting to 'onlinechat' database.
 */
public class SQLConnection {
    String ip = "localhost";
    String port = "3306";
    String andmebaas = "onlinechat";
    String andmebaasiKasutaja = "kasutaja";
    String andmebaasiParool = "paroolike"; // todo need tuleb ka encryptida, arvutist sisse lugeda?

    private int registreeri(String kasutajaNimi, String parool) throws SQLException {
        try (Connection ühendus = DriverManager.getConnection("jdbc:mysql://" + ip + ":" + port + "/" + andmebaas, andmebaasiKasutaja, andmebaasiParool)) {

            PreparedStatement stmt = ühendus.prepareStatement("CALL sp_create_user(?,?)");
            stmt.setString(1, kasutajaNimi);

            try {
                String turvalineParool = BCrypt.hashpw(parool, BCrypt.gensalt());
                stmt.setString(2, turvalineParool);
                stmt.executeQuery();
            } catch (SQLIntegrityConstraintViolationException e) {
                e.printStackTrace();
                return 1;
            }
            return 0;
        }
    }

    private int login(String kasutajaNimi, String s_parool) throws SQLException {
        try (Connection ühendus = DriverManager.getConnection("jdbc:mysql://" + ip + ":" + port + "/" + andmebaas, andmebaasiKasutaja, andmebaasiParool)) {
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
                e.printStackTrace();
            }
            return 1;
        }
    }
}