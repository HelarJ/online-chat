package ee.oop.onlinechat;

import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;

public class CommandHandler {
    private Sender sender;
    private ClientInfo client;
    private SocketChannel socketChannel;
    private String[] answerParts;

    public CommandHandler(ClientInfo client, SocketChannel socketChannel, Sender sender) {
        this.client = client;
        this.sender = sender;
        this.socketChannel = socketChannel;
    }

    /**
     * Tries to parse the command the user has selected (with the character '/').
     * Available commands as of the moment: /help, /register, /login, /logout, /exit, /createchannel, /joinchannel, /leavechannel, /send
     *
     * @param answer line of text received from user, starting with '/' and used to define
     *               the selected command as well as additional needed parameters.
     */
    public void handleCommand(String answer) {
        answerParts = answer.split(" ");
        Command command = null;
        try {
            command = Command.valueOf(answerParts[0].toUpperCase().substring(1)); //Saadud commande võrreldakse Command enumiga.
        } catch (IllegalArgumentException e) { //See exception visatakse, kui commandi pole enumis.
            Server.logger.info(String.format("Client %s attempted to use command %s which does not exist.", client.getName(), answerParts[0]));
            sender.sendText("Command " + answerParts[0] + " does not exist.", socketChannel);
        }

        if (command == null) {
            return;
        }
        switch (command) {
            case HELP:
                handleHelp();
                break;
            case REGISTER:
                handleRegister();
                break;
            case LOGIN:
                handleLogin();
                break;
            case LOGOUT:
                handleLogout();
                break;
            case HISTORY:
                handleHistory();
                break;
            case CREATECHANNEL:
                handleCreateChannel();
                break;
            case JOINCHANNEL:
                handleJoinChannel();
                break;
            case LEAVECHANNEL:
                handleLeaveChannel();
                break;
            case SEND: //spetsiifilisse kanalisse või inimesele sõnumi saatmine
                handleSend();
                break;
        }
    }

    private void handleHelp(){
        StringBuilder sb = new StringBuilder();
        sb.append("Avaliable commands: ");
        for (Command cmd: Command.values()){
            sb.append("/");
            sb.append(cmd.toString());
            sb.append(", ");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.deleteCharAt(sb.length()-1);
        System.out.println(sb.toString());
        sender.sendText(sb.toString(), socketChannel);
    }

    private void handleRegister(){
        if (client.isLoggedIn()){
            String alreadyLoggedIn = "This account is logged in! Please /logout before registering again.";
            sender.sendText(alreadyLoggedIn, socketChannel);
            return;
        }

        if (answerParts.length != 3) {
            String wrongArgs = "Invalid arguments for this command! Correct syntax: /register [username] [password]";
            sender.sendText(wrongArgs, socketChannel);
        }
        if (answerParts[1].length() > 29) { // kui kasutajanimi on pikem kui 29 characterit, keelatakse kasutaja loomine
            String tooLongUsername = "Entered username is too long! Please use a username with 29 characters or less.";
            sender.sendText(tooLongUsername, socketChannel);
        } else {
            SQLConnection sqlConnection = new SQLConnection();
            SQLResponse response = sqlConnection.register(answerParts[1], answerParts[2], Command.REGISTER);
            if (response == SQLResponse.SUCCESS) {
                Server.logger.info(String.format("Account %s registered successfully.", answerParts[1]));
                String accCreated = "Account created! Please /login [username] [password]!";
                sender.sendText(accCreated, socketChannel);
                forceChannelMainAndUsername(answerParts[1]);
            } else if (response == SQLResponse.DUPLICATE) {
                String accExists = "An account (or channel) with this name already exists. Try a different username.";
                sender.sendText(accExists, socketChannel);
            } else if (response == SQLResponse.ERROR) {
                String connectionError = "Error connecting to the database. Please try again later.";
                sender.sendText(connectionError, socketChannel);
            }
        }
    }

    private void handleLogin(){
        if (client.isLoggedIn()){
            String alreadyLoggedIn = "This account is logged in! Please /logout before logging in again.";
            sender.sendText(alreadyLoggedIn, socketChannel);
            return;
        }

        if (answerParts.length != 3) {
            String wrongArgs = "Invalid arguments for this command! Correct syntax: /login [username] [password]";
            sender.sendText(wrongArgs, socketChannel);
        } else {
            SQLConnection sqlConnection = new SQLConnection();
            SQLResponse response = sqlConnection.login(answerParts[1], answerParts[2], Command.LOGIN);
            if (response == SQLResponse.SUCCESS) {
                String loginSuccessful = "Login successful. Use /help to see other commands.";
                sender.sendText(loginSuccessful, socketChannel);
                client.setName(answerParts[1]);
                client.setLoggedIn(true);
                Server.logger.info(String.format("User %s logged in.", client.getName()));

                //fetchime kõik channelid, millega kasutaja ühendatud on
                sqlConnection = new SQLConnection();
                List<String> connectedChannels = sqlConnection.getJoinedChannels(client.getName());
                for (String channel : connectedChannels) {
                    client.joinChannel(channel);
                    sender.sendChatLog(socketChannel, channel, 100); //saadab sisselogimisel vanad sõnumid.
                }
            } else if (response == SQLResponse.DOESNOTEXIST) {
                sender.sendText("No account with this name found. Please /register [username] [password] to continue.", socketChannel);
            } else if (response == SQLResponse.WRONGPASSWORD) {
                Server.logger.info(String.format("Wrong password entered for account %s.", answerParts[1]));
                String invalidLogin = "Wrong password! Please try again.";
                sender.sendText(invalidLogin, socketChannel);
            } else if (response == SQLResponse.ERROR) {
                String connectionError = "Error connecting to the database. Please try again later.";
                sender.sendText(connectionError, socketChannel);
            }
        }
    }
    private void handleLogout(){
        if (checkIfNotLoggedIn()){
            return;
        }
        sender.sendText("Logged out.", socketChannel);
        client.setLoggedIn(false);
        client.setName("Default");
    }

    private void handleHistory(){
        if (checkIfNotLoggedIn()){
            return;
        }

        if (answerParts.length != 3) {
            String wrongArgs = "You have either entered too few or too many arguments. Correct syntax: /history [channel] [amount]";
            sender.sendText(wrongArgs, socketChannel);
        } else {
            if (!client.isInChannel(answerParts[1])) {
                sender.sendText("You are attempting to get the message history of a channel you are not currently in", socketChannel);
                return;
            }
            try {
                int amount = Integer.parseInt(answerParts[2]);
                if (amount > 500 || amount < 0) {
                    String tooManyMsg = "The number you have entered is either too big or too small! Please enter" +
                            " a number between 0 and 500";
                    sender.sendText(tooManyMsg, socketChannel);
                } else {
                    sender.sendChatLog(socketChannel, answerParts[1], amount);
                }
            } catch (NumberFormatException e) {
                String incorrectNum = "Incorrect number of messages to return. Please try again.";
                sender.sendText(incorrectNum, socketChannel);
            }
        }
    }

    private void handleCreateChannel(){
        if (checkIfNotLoggedIn()){
            return;
        }

        SQLConnection sqlConnection = new SQLConnection();
        SQLResponse response = SQLResponse.ERROR;
        switch (answerParts.length) {
            case 2: //paroolita kanali registreerimine
                if (answerParts[1].length() > 29) { // kui kanali nimi on pikem kui 29 characterit, keelatakse kanali loomine
                    sender.sendText("Entered channel name is too long! Please use a name with 29 characters or less.", socketChannel);
                    break;
                }
                response = sqlConnection.register(answerParts[1], null, Command.CREATECHANNEL);
                break;
            case 3: //parooliga kanali registreerimine
                if (answerParts[1].length() > 29) { // kui kanali nimi on pikem kui 29 characterit, keelatakse kanali loomine
                    sender.sendText("Entered channel name is too long! Please use a name with 29 characters or less.", socketChannel);
                    break;
                }
                response = sqlConnection.register(answerParts[1], answerParts[2], Command.CREATECHANNEL);
                break;
            default:
                sender.sendText("Invalid arguments for this command! Correct syntax: /createchannel [channelname] (optional)[password]", socketChannel);
                break;
        }

        switch (response) {
            case SUCCESS:
                sender.addChannel(answerParts[1]);
                sender.sendText("Channel created successfully! Use /joinchannel " + answerParts[1] + " to join it.", socketChannel);
                Server.logger.info(String.format("User %s created a new channel %s.", client.getName(), answerParts[1]));
                break;
            case DUPLICATE:
                sender.sendText("A channel or user with this name already exists. Use /joinchannel [channel] [password](optional) to join the channel.", socketChannel);
                break;
            case ERROR:
                sender.sendText("Error connecting to the database. Please try again later.", socketChannel);
                break;
        }
    }
    private void handleJoinChannel(){
        if (checkIfNotLoggedIn()){
            return;
        }
        SQLConnection sqlConnection = new SQLConnection();
        SQLResponse response = SQLResponse.ERROR;
        switch (answerParts.length) {
            case 2: //paroolita kanaliga liitumine
                if (sqlConnection.isUserChannel(answerParts[1])) { // kui on kasutaja, siis sinna ühineda ei saa
                    break;
                }
                response = sqlConnection.login(answerParts[1], null, Command.JOINCHANNEL);
                if (response == SQLResponse.ERROR) {
                    break;
                }
                response = sqlConnection.addUserToChannel(client.getName(), answerParts[1]);
                break;
            case 3: //parooliga kanaliga liitumine
                if (sqlConnection.isUserChannel(answerParts[1])) { // kui on kasutaja, siis sinna ühineda ei saa
                    break;
                }
                response = sqlConnection.login(answerParts[1], answerParts[2], Command.JOINCHANNEL);
                if (response == SQLResponse.ERROR) {
                    break;
                }
                response = sqlConnection.addUserToChannel(client.getName(), answerParts[1]);
                break;
            default:
                sender.sendText("Invalid arguments for this command! Correct syntax: /joinchannel [channelname] [password](optional)", socketChannel);
                break;
        }

        switch (response) {
            case SUCCESS:
                client.joinChannel(answerParts[1]);
                sender.sendText("Joined channel " + answerParts[1] + ". Use /send " + answerParts[1] + " [message] to talk in the channel.", socketChannel);
                sender.sendChatLog(socketChannel, answerParts[1], 100);
                Server.logger.info(String.format("User %s joined channel %s.", client.getName(), answerParts[1]));
                break;
            case DOESNOTEXIST:
                sender.sendText("No channel with this name found. Please use /createchannel [name] [password](optional)", socketChannel);
                break;
            case WRONGPASSWORD:
                Server.logger.info(String.format("User %s attempted to join channel %s with the wrong password.", client.getName(), answerParts[1]));
                sender.sendText("Wrong password given for this channel.", socketChannel);
                break;
            case ERROR:
                sender.sendText("Error connecting to the database. Please try again later.", socketChannel);
                break;
        }
    }
    private void handleLeaveChannel(){
        if (checkIfNotLoggedIn()){
            return;
        }
        SQLConnection sqlConnection = new SQLConnection();

        if (answerParts.length != 2) {
            sender.sendText("Wrong syntax for this command. Use /leavechannel [channel]", socketChannel);
        } else if (client.isInChannel(answerParts[1]) && !sqlConnection.isUserChannel(answerParts[1])) {
            SQLResponse response = sqlConnection.removeUserFromChannel(client.getName(), answerParts[1]);

            if (response != SQLResponse.ERROR) {
                sender.sendText(String.format("Left channel %s.", answerParts[1]), socketChannel);
                client.leaveChannel(answerParts[1]);
            } else {
                sender.sendText("Error connecting to the database. Please try again later.", socketChannel);
            }
        } else {
            sender.sendText(String.format("Not in channel %s.", answerParts[1]), socketChannel);
        }
    }

    private void handleSend(){
        if (checkIfNotLoggedIn()){
            return;
        }
        SQLConnection sqlConnection = new SQLConnection();
        switch (answerParts.length) {
            case 1:
                sender.sendText("The proper syntax for this command is /send [channel] [message]", socketChannel);
                break;
            case 2:
                if (client.isInChannel(answerParts[1]) && !sqlConnection.isUserChannel(answerParts[1])) {
                    sender.sendText("You have joined this channel, but you need to put in a message as well. /send [channel] [message]", socketChannel);
                } else if (sqlConnection.isUserChannel(answerParts[1])) {
                    sender.sendText("You have attempted to send an empty message to an user. /send [user] [message]", socketChannel);
                } else if (!client.isInChannel(answerParts[1]) && !sqlConnection.isUserChannel(answerParts[1])) {
                    sender.sendText("You have attempted to send a message to a channel you are not a part of.", socketChannel);
                }
                break;
            default:
                if (client.isInChannel(answerParts[1]) && !sqlConnection.isUserChannel(answerParts[1])) {
                    String channel = answerParts[1];
                    String[] messageParts = Arrays.copyOfRange(answerParts, 2, answerParts.length);
                    sender.sendMsgWithSenderToChannel(channel, client, String.join(" ", messageParts));

                } else if (sqlConnection.isUserChannel(answerParts[1])) {
                    String channel = answerParts[1];
                    String[] messageParts = Arrays.copyOfRange(answerParts, 2, answerParts.length);
                    sender.sendMsgWithSenderToChannel(channel, client, String.join(" ", messageParts));
                } else {
                    sender.sendText("You are attempting to send a message to a channel you have not joined. Use /joinchannel [name] [password]", socketChannel);
                }
        }


    }

    /**
     * Checks if the current client is offline and if they are sends them a message prompting them to log in
     * @return Returns TRUE if client is NOT logged in.
     */
    private boolean checkIfNotLoggedIn(){
        if (!client.isLoggedIn()){
            String loginRequest = "Please log in before requesting this command!";
            sender.sendText(loginRequest, socketChannel);
            return true;
        }
        return false;
    }

    private void forceChannelMainAndUsername(String name) {
        SQLConnection sqlConnection = new SQLConnection();
        SQLResponse response;
        response = sqlConnection.addUserToChannel(name, "Main");
        if (response == SQLResponse.ERROR) {
            sender.sendText("Error connecting to the database. Please try again later.", socketChannel);
        }
        response = sqlConnection.addUserToChannel(name, name);

        switch (response) {
            case SUCCESS:
                client.joinChannel("Main");
                Server.logger.info(String.format("User %s joined channel %s.", client.getName(), "Main and " + name));
                break;
            case ERROR:
                sender.sendText("Error connecting to the database. Please try again later.", socketChannel);
                break;
        }
    }
}
