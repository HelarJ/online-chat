package ee.oop.onlinechat;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
            Server.logger.severe("Config file not found for database.");
            Server.logger.severe(e.getMessage());
        } catch (IOException e) {
            Server.logger.severe("Error reading properties from config file.");
            Server.logger.severe(e.getMessage());
        }
    }

    public SQLResponse register(String kasutajaNimi, String parool, Command registerType) {
        String statementStr;
        if (registerType == Command.REGISTER) {
            statementStr = "CALL sp_create_user(?,?)";
        } else if (registerType == Command.CREATECHANNEL) {
            statementStr = "CALL sp_create_channel(?,?)";
        } else {
            return SQLResponse.ERROR;
        }
        try (Connection ühendus = DriverManager.getConnection(credentials);
             PreparedStatement stmt = ühendus.prepareStatement(statementStr)) {
            stmt.setString(1, kasutajaNimi);

            try {
                if (registerType == Command.CREATECHANNEL && parool == null) { //kui tegu on kanali registreeringuga siis on võimalik et parooli ei ole.
                    stmt.setNull(2, java.sql.Types.NULL);
                } else {
                    String turvalineParool = BCrypt.hashpw(parool, BCrypt.gensalt());
                    stmt.setString(2, turvalineParool);
                }
                stmt.executeQuery();
            } catch (SQLException e) {
                if (e.getErrorCode() == 1062 || e.getErrorCode() == 1305) { //1062 & 1305 are duplicate entry errorcodes.. ehk kui kasutaja või channel on juba andmebaasis olemas.
                    return SQLResponse.DUPLICATE;
                }
                Server.logger.severe("Unexpected error while registering a user/channel.");
                Server.logger.severe("SQL ErrorCode: " + e.getErrorCode() + ", Message: " + e.getMessage());
                return SQLResponse.ERROR;
            }
            return SQLResponse.SUCCESS;
        } catch (SQLException e) {
            Server.logger.severe("Error connecting to the database.");
            Server.logger.severe("SQL ErrorCode: " + e.getErrorCode() + ", Message: " + e.getMessage());
            return SQLResponse.ERROR;
        }
    }

    public SQLResponse login(String kasutajaNimi, String s_parool, Command loginType) {
        String statementStr;
        if (loginType == Command.LOGIN) {
            statementStr = "CALL sp_login(?)";
        } else if (loginType == Command.JOINCHANNEL) {
            statementStr = "CALL sp_join_channel(?)";
        } else {
            return SQLResponse.ERROR;
        }
        try (Connection ühendus = DriverManager.getConnection(credentials);
             PreparedStatement stmt = ühendus.prepareStatement(statementStr)) {
            stmt.setString(1, kasutajaNimi);

            try {
                ResultSet rs = stmt.executeQuery();
                rs.next();
                String Token = rs.getString("parool");

                if (loginType == Command.JOINCHANNEL && Token == null) { //Paroolita kanaliga ühinemisel saab andmebaasist null väärtuse.
                    return SQLResponse.SUCCESS;
                }

                if (BCrypt.checkpw(s_parool, Token)) {
                    return SQLResponse.SUCCESS;
                } else {
                    return SQLResponse.WRONGPASSWORD;
                }
            } catch (SQLException e) {
                if (e.getErrorCode() == 0) {
                    return SQLResponse.DOESNOTEXIST;
                }
                Server.logger.severe("Unexpected error while logging in.");
                Server.logger.severe("SQL ErrorCode: " + e.getErrorCode() + ", Message: " + e.getMessage());
                return SQLResponse.ERROR;

            }
        } catch (SQLException e) {
            Server.logger.severe("Error connecting to the database.");
            Server.logger.severe("SQL ErrorCode: " + e.getErrorCode() + ", Message: " + e.getMessage());
            return SQLResponse.ERROR;
        }
    }

    public void logMessage(String channelName, Message msg) {
        try (Connection ühendus = DriverManager.getConnection(credentials);
             PreparedStatement stmt = ühendus.prepareStatement("CALL sp_log_message(?,?,?)")) {
            stmt.setString(1, channelName);
            stmt.setString(2, msg.getUsername());
            stmt.setString(3, msg.getMessage());
            stmt.executeUpdate();

        } catch (SQLException e) {
            Server.logger.severe("Error connecting to the database.");
            Server.logger.severe("SQL ErrorCode: " + e.getErrorCode() + ", Message: " + e.getMessage());
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

        } catch (SQLException e) {
            Server.logger.severe("Error connecting to the database.");
            Server.logger.severe("SQL ErrorCode: " + e.getErrorCode() + ", Message: " + e.getMessage());
        }
        Collections.reverse(messageList);
        return messageList;
    }

    public List<String> getChannels() {
        List<String> channelList = new ArrayList<>();
        try (Connection ühendus = DriverManager.getConnection(credentials);
             PreparedStatement stmt = ühendus.prepareStatement("CALL sp_get_channel_list()")) {
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                channelList.add(resultSet.getString("nimi"));
            }
        } catch (SQLException e) {
            Server.logger.severe("Error connecting to the database.");
            Server.logger.severe("SQL ErrorCode: " + e.getErrorCode() + ", Message: " + e.getMessage());
        }
        return channelList;
    }

    public List<String> getJoinedChannels(String user) {
        List<String> channelList = new ArrayList<>();
        try (Connection ühendus = DriverManager.getConnection(credentials);
             PreparedStatement stmt = ühendus.prepareStatement("CALL sp_get_joined_channels(?)")) {
            stmt.setString(1, user);
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                channelList.add(resultSet.getString("nimi"));
            }
        } catch (SQLException e) {
            Server.logger.severe("Error connecting to the database.");
            Server.logger.severe("SQL ErrorCode: " + e.getErrorCode() + ", Message: " + e.getMessage());
        }
        return channelList;
    }

    public boolean isUserChannel(String channelName) {
        try (Connection ühendus = DriverManager.getConnection(credentials);
             PreparedStatement stmt = ühendus.prepareStatement("CALL sp_is_user_channel(?)")) {
            stmt.setString(1, channelName);
            ResultSet resultSet = stmt.executeQuery();
            resultSet.next();
            int vastus = resultSet.getInt("onKasutaja");
            return vastus == 1;
        } catch (SQLException e) {
            Server.logger.severe("Error connecting to the database.");
            Server.logger.severe("SQL ErrorCode: " + e.getErrorCode() + ", Message: " + e.getMessage());
        }
        return false;
    }


    public SQLResponse addUserToChannel(String clientName, String channelName) {
        try (Connection ühendus = DriverManager.getConnection(credentials);
             PreparedStatement userCreate = ühendus.prepareStatement("CALL sp_add_user_to_channel(?,?)")) {
            userCreate.setString(1, channelName);
            userCreate.setString(2, clientName);
            userCreate.executeQuery();
            return SQLResponse.SUCCESS;
        } catch (SQLException e) {
            Server.logger.severe("Error connecting to the database.");
            Server.logger.severe("SQL ErrorCode: " + e.getErrorCode() + ", Message: " + e.getMessage());
            return SQLResponse.ERROR;
        }
    }

    public SQLResponse removeUserFromChannel(String clientName, String channelName) {
        try (Connection ühendus = DriverManager.getConnection(credentials);
             PreparedStatement userDelete = ühendus.prepareStatement("CALL sp_remove_user_from_channel(?,?)")) {
            userDelete.setString(1, channelName);
            userDelete.setString(2, clientName);
            userDelete.executeQuery();
            return SQLResponse.SUCCESS;
        } catch (SQLException e) {
            Server.logger.severe("Error connecting to the database.");
            Server.logger.severe("SQL ErrorCode: " + e.getErrorCode() + ", Message: " + e.getMessage());
            return SQLResponse.ERROR;
        }
    }
}