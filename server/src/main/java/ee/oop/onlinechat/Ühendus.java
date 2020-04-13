package ee.oop.onlinechat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public class Ühendus {
    private Selector selector;
    private Map<SocketChannel, ClientInfo> dataMapper;
    private InetSocketAddress listenAddress;
    private ChatLog chatLog;
    private SQLConnection sqlConnection;

    public Ühendus(String aadress, int port) {
        listenAddress = new InetSocketAddress(aadress, port);
        dataMapper = new HashMap<>();
        chatLog = new ChatLog("MainChannel");
    }

    public void ühenda() throws IOException {
        this.selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        serverChannel.socket().bind(listenAddress);
        serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started...");
        Runnable sulgeja = () -> {
            Scanner sc = new Scanner(System.in);
            String command = "";
            while (!command.equals("/exit")) {
                command = sc.nextLine();
            }
            Server.shutdown();
        };
        new Thread(sulgeja).start();

        while (Server.isRunning()) {
            this.selector.select();
            Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();
                if (!key.isValid()) {
                    continue;
                }
                if (key.isAcceptable()) {
                    this.accept(key);
                } else if (key.isReadable()) {
                    this.read(key);
                }
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        System.out.println("Connected to: " + remoteAddr);
        String greeting = "Welcome to blablaChat! To start using chat, please use command /register [username] [password]!";
        dataMapper.put(channel, new ClientInfo()); //jätab alguses channeli meelde koos default nimega.
        channel.register(this.selector, SelectionKey.OP_READ);
        sendTextBack(greeting, channel);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        Socket s = channel.socket();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int count;
        byte[] data;
        try {
            count = channel.read(buffer);

            data = new byte[count];
            System.arraycopy(buffer.array(), 0, data, 0, count);
            String vastus = new String(data);
            System.out.println("Got: " + vastus);

            if (vastus.substring(0, 1).equals("/")) { // kui oli command
                commandSwitch(vastus, channel);
            } else if (dataMapper.get(channel).isLoggedIn()) { // kasutaja on sisse logitud
                sendMsgWithSenderToAll(dataMapper.get(channel), vastus); //saadab kõigile channelitele todo (võib-olla peab panema tagasi try seest välja ja lisama IF command checki (et ei saadaks kõigile kirjutatud commandi))
            } else { // kasutaja pole sisse logitud
                String loginRequest = "Please log in or create an account before sending messages with /register [username] [password]!";
                sendTextBack(loginRequest, channel);
            }
        } catch (IOException e) {
            dataMapper.get(channel).setLoggedIn(false);
            this.dataMapper.remove(channel);
            System.out.printf("Connection closed by client: %s%n", s.getRemoteSocketAddress());
            channel.close();
            key.cancel();
        }
    }


    /*private void sendToAll(ByteBuffer data) { // saab hiljem kasutada, kui saadame pilte ja asju
        dataMapper.forEach((c, d) -> { //saadab igale ühendusele, mis on kaardistatud dataMapperis.
            try {
                c.write(data);
                System.out.printf("Sent: %s to %s%n at %s%n", new String(data.array()), d.getNimi(), c.getRemoteAddress());
                data.rewind();
            } catch (IOException e) {
                System.out.println("Error sending to channel: " + e.getMessage());
            }
        });
    }*/
    private void sendMsgWithSenderToAll(ClientInfo saatja, String text) { // sama asi mis sendToAll, aga selle asemel et saadab byteBufferi siis saadab stringi
        Message msg = new Message(saatja.getName(), text);
        chatLog.logMessage(msg); //Logitakse ainult need sõnumid, mida nagunii kõigile oleks saadetud.
        String msgToSend = msg.toString();
        byte[] bytes = msgToSend.getBytes();
        ByteBuffer data = ByteBuffer.wrap(bytes);
        dataMapper.forEach((c, d) -> { //saadab igale ühendusele, mis on kaardistatud dataMapperis.
            try {
                c.write(data);
                System.out.printf("Sent: %s to %s%n at %s%n", new String(data.array()), d.getName(), c.getRemoteAddress());
                data.rewind();
            } catch (IOException e) {
                System.out.println("Error sending to channel: " + e.getMessage());
            }
        });
    }

    private void sendTextBack(String text, SocketChannel c) {
        byte[] tekstBytes = text.getBytes();
        ByteBuffer data = ByteBuffer.wrap(tekstBytes);
        try {
            c.write(data);
            System.out.printf("Sent: %s to %s%n at %s%n", new String(data.array()), dataMapper.get(c).getName(), c.getRemoteAddress());
            data.rewind();
        } catch (IOException e) {
            System.out.println("Error sending to channel: " + e.getMessage());
        }
    }

    /**
     * Sends (up to) @param amount of last logged messages to specified channel.
     * @param c channel to send to.
     * @param amount of messages to return to the specified channel.
     */
    private void sendChatLog(SocketChannel c, int amount) {
        for (Message msg : chatLog.getLastMessages(amount)) {
            sendTextBack(msg.toString(), c);
        }
    }

    /**
     * Tries to parse the command the user has selected (with the character '/').
     * Available commands as of the moment: help, register, login, logout, history
     *
     * @param answer  line of text received from user, starting with '/' and used to define
     *                the selected command as well as additional needed parameters.
     * @param channel SocketChannel where the line of text (answer) was received from and
     *                used to send information back to the same channel.
     */
    private void commandSwitch(String answer, SocketChannel channel) {
        String[] answerParts = answer.split(" ");
        switch (answerParts[0]) {

            case "/help":
                String commands = "Available commands: /help, /register, /login, /exit";
                sendTextBack(commands, channel);
                break;

            case "/register":
                if (!dataMapper.get(channel).isLoggedIn()) {
                    if (answerParts.length != 3) {
                        String wrongArgs = "Invalid arguments for this command! Correct syntax: /register [username] [password]";
                        sendTextBack(wrongArgs, channel);
                    } else {
                        SQLConnection sqlConnection = new SQLConnection();
                        int response = sqlConnection.register(answerParts[1], answerParts[2]);
                        if (response == 1) {
                            String accExists = "An account with this name already exists. Use /login [username] [password]";
                            sendTextBack(accExists, channel);
                        } else if (response == 0) {
                            String accCreated = "Account created! You may start sending messages!";
                            sendTextBack(accCreated, channel);
                            dataMapper.get(channel).setName(answerParts[1]);
                            dataMapper.get(channel).setLoggedIn(true);
                        }
                    }
                } else {
                    String alreadyLoggedIn = "This account is logged in! Please /logout before registering again.";
                    sendTextBack(alreadyLoggedIn, channel);
                }
                break;

            case "/login":
                if (!dataMapper.get(channel).isLoggedIn()) {
                    if (answerParts.length != 3) {
                        String wrongArgs = "Invalid arguments for this command! Correct syntax: /login [username] [password]";
                        sendTextBack(wrongArgs, channel);
                    } else {
                        SQLConnection sqlConnection = new SQLConnection();
                        int response = sqlConnection.login(answerParts[1], answerParts[2]);
                        if (response == 1) {
                            String invalidLogin = "Invalid login! Please try again.";
                            sendTextBack(invalidLogin, channel);
                        } else if (response == 0) {
                            String loginSuccessful = "Login successful.";
                            sendTextBack(loginSuccessful, channel);
                            dataMapper.get(channel).setName(answerParts[1]);
                            dataMapper.get(channel).setLoggedIn(true);
                            sendChatLog(channel, 100); //saadab sisselogimisel vanad sõnumid.
                        }
                    }
                } else {
                    String alreadyLoggedIn = "This account is logged in! Please /logout before logging in again.";
                    sendTextBack(alreadyLoggedIn, channel);
                }
                break;

            case "/logout":
                String logOut = "Logged out.";
                sendTextBack(logOut, channel);
                dataMapper.get(channel).setLoggedIn(false);
                dataMapper.get(channel).setName("Default");
                break;

            case "/history":
                if (dataMapper.get(channel).isLoggedIn()) {
                    if (answerParts.length != 2) {
                        String wrongArgs = "You have either entered too few or too many arguments. Correct syntax: /history [amount]";
                        sendTextBack(wrongArgs, channel);
                    } else {
                        try {
                            int amount = Integer.parseInt(answerParts[1]);
                            if (amount > 500 || amount < 0) {
                                String tooManyMsg = "The number you have entered is either too big or too small! Please enter" +
                                        " a number between 0 and 500";
                                sendTextBack(tooManyMsg, channel);
                            } else {
                                sendChatLog(channel, amount);
                            }
                        } catch (NumberFormatException e) {
                            String incorrectNum = "Incorrect number of messages to return. Please try again.";
                            sendTextBack(incorrectNum, channel);
                        }
                    }
                } else {
                    String loginRequest = "Please log in before requesting this command!";
                    sendTextBack(loginRequest, channel);
                }
        }
    }
}