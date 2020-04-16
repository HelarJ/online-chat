package ee.oop.onlinechat;

import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class CommandHandler {
    private Sender sender;
    private ClientInfo client;
    private SocketChannel socketChannel;

    public CommandHandler(ClientInfo client, SocketChannel socketChannel, Sender sender) {
        this.client = client;
        this.sender = sender;
        this.socketChannel = socketChannel;
    }

    /**
     * Tries to parse the command the user has selected (with the character '/').
     * Available commands as of the moment: help, register, login, logout, history
     *
     * @param answer  line of text received from user, starting with '/' and used to define
     *                the selected command as well as additional needed parameters.
     */
    public void handleCommand(String answer){
        String[] answerParts = answer.split(" ");
        Command command = null;
        try {
            command = Command.valueOf(answerParts[0].toUpperCase().substring(1)); //Saadud commande võrreldakse Command enumiga.
        } catch (IllegalArgumentException e){ //See exception visatakse, kui commandi pole enumis.
            Server.logger.info(String.format("Client %s attempted to use command %s which does not exist.", client.getName(), answerParts[0]));
            sender.sendTextBack("Command "+answerParts[0]+" does not exist.", socketChannel);
        }

        if (command == null){
            return;
        }
        switch (command) {
            case HELP:
                String commands = "Available commands: /help, /register, /login, /exit /createchannel /joinchannel /leavechannel /send";
                sender.sendTextBack(commands, socketChannel);
                break;
            case REGISTER:
                if (!client.isLoggedIn()) {
                    if (answerParts.length != 3) {
                        String wrongArgs = "Invalid arguments for this command! Correct syntax: /register [username] [password]";
                        sender.sendTextBack(wrongArgs, socketChannel);
                    } else {
                        SQLConnection sqlConnection = new SQLConnection();
                        SQLResponse response = sqlConnection.register(answerParts[1], answerParts[2], Command.REGISTER);
                        if (response == SQLResponse.SUCCESS) {
                            Server.logger.info(String.format("Account %s registered successfully.", answerParts[1]));
                            String accCreated = "Account created! Please /login [username] [password]!";
                            sender.sendTextBack(accCreated, socketChannel);
                        } else if (response == SQLResponse.DUPLICATE) {
                            String accExists = "An account with this name already exists. Use /login [username] [password]";
                            sender.sendTextBack(accExists, socketChannel);
                        } else if (response == SQLResponse.ERROR){
                            String connectionError = "Error connecting to the database. Please try again later.";
                            sender.sendTextBack(connectionError, socketChannel);
                        }
                    }
                } else {
                    String alreadyLoggedIn = "This account is logged in! Please /logout before registering again.";
                    sender.sendTextBack(alreadyLoggedIn, socketChannel);
                }
                break;

            case LOGIN:
                if (!client.isLoggedIn()) {
                    if (answerParts.length != 3) {
                        String wrongArgs = "Invalid arguments for this command! Correct syntax: /login [username] [password]";
                        sender.sendTextBack(wrongArgs, socketChannel);
                    } else {
                        SQLConnection sqlConnection = new SQLConnection();
                        SQLResponse response = sqlConnection.login(answerParts[1], answerParts[2], Command.LOGIN);
                        if (response == SQLResponse.SUCCESS) {
                            String loginSuccessful = "Login successful.";
                            sender.sendTextBack(loginSuccessful, socketChannel);
                            client.setName(answerParts[1]);
                            client.setLoggedIn(true);
                            Server.logger.info(String.format("User %s logged in.", client.getName()));
                            client.joinChannel("Main"); //todo kui andmebaasi lisada kanalid milles kasutaja on, siis saaks automaatselt nendega sisselogimisel ühineda.
                            sender.sendChatLog(socketChannel, "Main", 100); //saadab sisselogimisel vanad sõnumid.
                        } else if (response == SQLResponse.DOESNOTEXIST){
                            sender.sendTextBack("No account with this name found. Please /register [username] [password] to continue.", socketChannel);
                        } else if (response == SQLResponse.WRONGPASSWORD) {
                            Server.logger.info(String.format("Wrong password entered for account %s.", answerParts[1]));
                            String invalidLogin = "Wrong password! Please try again.";
                            sender.sendTextBack(invalidLogin, socketChannel);
                        } else if (response == SQLResponse.ERROR){
                            String connectionError = "Error connecting to the database. Please try again later.";
                            sender.sendTextBack(connectionError, socketChannel);
                        }
                    }
                } else {
                    String alreadyLoggedIn = "This account is logged in! Please /logout before logging in again.";
                    sender.sendTextBack(alreadyLoggedIn, socketChannel);
                }
                break;

            case LOGOUT:
                sender.sendTextBack("Logged out.", socketChannel);
                client.setLoggedIn(false);
                client.setName("Default");
                break;

            case HISTORY:
                if (client.isLoggedIn()) {
                    if (answerParts.length != 3) {
                        String wrongArgs = "You have either entered too few or too many arguments. Correct syntax: /history [channel] [amount]";
                        sender.sendTextBack(wrongArgs, socketChannel);
                    } else {
                        if (!client.isInChannel(answerParts[1])){
                            sender.sendTextBack("You are attempting to get the message history of a channel you are not currently in", socketChannel);
                            break;
                        }
                        try {
                            int amount = Integer.parseInt(answerParts[2]);
                            if (amount > 500 || amount < 0) {
                                String tooManyMsg = "The number you have entered is either too big or too small! Please enter" +
                                        " a number between 0 and 500";
                                sender.sendTextBack(tooManyMsg, socketChannel);
                            } else {
                                sender.sendChatLog(socketChannel, answerParts[1], amount);
                            }
                        } catch (NumberFormatException e) {
                            String incorrectNum = "Incorrect number of messages to return. Please try again.";
                            sender.sendTextBack(incorrectNum, socketChannel);
                        }
                    }
                } else {
                    String loginRequest = "Please log in before requesting this command!";
                    sender.sendTextBack(loginRequest, socketChannel);
                }
                break;
            case CREATECHANNEL:
                if (client.isLoggedIn()){
                    SQLConnection sqlConnection = new SQLConnection();
                    SQLResponse response = SQLResponse.ERROR;
                    switch (answerParts.length){
                        case 2: //paroolita kanali registreerimine
                            response = sqlConnection.register(answerParts[1], null, Command.CREATECHANNEL);
                            break;
                        case 3: //parooliga kanali registreerimine
                            response = sqlConnection.register(answerParts[1], answerParts[2], Command.CREATECHANNEL);
                            break;
                        default:
                            sender.sendTextBack("Invalid arguments for this command! Correct syntax: /createchannel [channelname] (optional)[password]", socketChannel);
                            break;
                    }

                    switch (response){
                        case SUCCESS:
                            sender.addChannel(answerParts[1]);
                            sender.sendTextBack("Channel created successfully! Use /joinchannel "+answerParts[1] +" to join it.", socketChannel);
                            Server.logger.info(String.format("User %s created a new channel %s.", client.getName(), answerParts[1]));
                            break;
                        case DUPLICATE:
                            sender.sendTextBack("A channel with this name already exists. Use /joinchannel [channel] [password](optional) to join the channel.", socketChannel);
                            break;
                        case ERROR:
                            sender.sendTextBack("Error connecting to the database. Please try again later.", socketChannel);
                            break;
                    }
                } else {
                    String loginRequest = "Please log in before requesting this command!";
                    sender.sendTextBack(loginRequest, socketChannel);
                }
                break;

            case JOINCHANNEL:
                if (client.isLoggedIn()){
                    SQLConnection sqlConnection = new SQLConnection();
                    SQLResponse response = SQLResponse.ERROR;
                    switch (answerParts.length){
                        case 2: //paroolita kanaliga liitumine
                            response = sqlConnection.login(answerParts[1], null, Command.JOINCHANNEL);
                            break;
                        case 3: //parooliga kanaliga liitumine
                            response = sqlConnection.login(answerParts[1], answerParts[2], Command.JOINCHANNEL);
                            break;
                        default:
                            sender.sendTextBack("Invalid arguments for this command! Correct syntax: /joinchannel [channelname] [password](optional)", socketChannel);
                            break;
                    }

                    switch (response){
                        case SUCCESS:
                            client.joinChannel(answerParts[1]);
                            sender.sendTextBack("Joined channel "+ answerParts[1] + ". Use /send "+answerParts[1]+" [message] to talk in the channel.", socketChannel);
                            sender.sendChatLog(socketChannel, answerParts[1], 100);
                            Server.logger.info(String.format("User %s joined channel %s.", client.getName(), answerParts[1]));
                            break;
                        case DOESNOTEXIST:
                            sender.sendTextBack("No channel with this name found. Please use /createchannel [name] [password](optional)", socketChannel);
                            break;
                        case WRONGPASSWORD:
                            Server.logger.info(String.format("User %s attempted to join channel %s with the wrong password.", client.getName(), answerParts[1]));
                            sender.sendTextBack("Wrong password given for this channel.", socketChannel);
                            break;
                        case ERROR:
                            sender.sendTextBack("Error connecting to the database. Please try again later.", socketChannel);
                            break;
                    }


                } else {
                    String loginRequest = "Please log in before requesting this command!";
                    sender.sendTextBack(loginRequest, socketChannel);
                }
                break;
            case LEAVECHANNEL:
                if (client.isLoggedIn()){
                    if (answerParts.length !=2){
                        sender.sendTextBack("Wrong syntax for this command. Use /leavechannel [channel]",socketChannel);
                    }

                    if (client.isInChannel(answerParts[1])){
                        client.leaveChannel(answerParts[1]);
                        sender.sendTextBack(String.format("Left channel %s.", answerParts[1]), socketChannel);
                    } else {
                        sender.sendTextBack(String.format("Not in channel %s.", answerParts[1]), socketChannel);
                    }

                } else {
                    String loginRequest = "Please log in before requesting this command!";
                    sender.sendTextBack(loginRequest, socketChannel);
                }
                break;
            case SEND: //spetsiifilisse kanalisse sõnumi saatmine
                if (client.isLoggedIn()){
                    switch (answerParts.length){
                        case 1:
                            sender.sendTextBack("The proper syntax for this command is /send [channel] [message]", socketChannel);
                            break;
                        case 2:
                            if (client.isInChannel(answerParts[1])){
                                sender.sendTextBack("You have joined this channel, but you need to put in a message as well. /send [channel] [message]", socketChannel);
                            } else {
                                sender.sendTextBack("You have attempted to send a message to a channel you are not a part of.", socketChannel);
                            }
                            break;
                        default:
                            if (client.isInChannel(answerParts[1])){
                                String channel = answerParts[1];
                                String[] messageParts = Arrays.copyOfRange(answerParts, 2, answerParts.length);
                                sender.sendMsgWithSenderToChannel(channel, client, String.join(" ", messageParts));

                            } else {
                                sender.sendTextBack("You are attempting to send a message to a channel you have not joined. Use /joinchannel [name] [password]", socketChannel);
                            }


                    }

                } else {
                    String loginRequest = "Please log in before requesting this command!";
                    sender.sendTextBack(loginRequest, socketChannel);
                }
                break;
        }
    }
}
