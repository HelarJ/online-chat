package ee.oop.onlinechat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ühendus {
    private Selector selector;
    private Map<SocketChannel, ClientInfo> socketClientMap;
    private InetSocketAddress listenAddress;

    private Sender sender;

    public Ühendus(String aadress, int port) {
        this.listenAddress = new InetSocketAddress(aadress, port);
        this.socketClientMap = new HashMap<>();
        this.sender = new Sender(socketClientMap);
    }

    public void ühenda() throws IOException {
        this.selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        serverChannel.socket().bind(listenAddress);
        serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        Server.logger.info("Server started...");

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
        Server.logger.severe("Server main loop has ended.");
        this.selector.close();
        serverChannel.close();

    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        Server.logger.info("Connected to: " + remoteAddr);
        socketClientMap.put(channel, new ClientInfo()); //jätab alguses channeli meelde koos default nimega.
        channel.register(this.selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ClientInfo client = socketClientMap.get(socketChannel);
        Socket s = socketChannel.socket();
        ByteBuffer buffer = ByteBuffer.allocate(256);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int count;
        try {
            while ((count = socketChannel.read(buffer)) > 0) {
                byte[] data = new byte[count];
                System.arraycopy(buffer.array(), 0, data, 0, count);
                bos.writeBytes(data);
                buffer.clear();
            }
            String vastus = new String(bos.toByteArray(), StandardCharsets.UTF_8);

            if (client.isWs()){
                vastus = new String(decode(buffer));
            }
            if (client.getClientType() == null){
                setClientType(socketChannel, vastus);
                String greeting = "Welcome to blablaChat! To start chatting, please /register [username] [password] or /login [username] [password]!";
                sender.sendText(greeting, socketChannel);
            } else {
                Server.logger.info("Got: " + vastus + " from " + socketChannel.getRemoteAddress());
                try {
                    if (vastus.substring(0, 1).equals("/")) { // kui oli command
                        CommandHandler commandHandler = new CommandHandler(client, socketChannel, sender); //handler saab clientinfo, socketChanneli ja sender objekti mida kasutada.
                        commandHandler.handleCommand(vastus);
                    } else if (client.isLoggedIn()) { // kasutaja on sisse logitud
                        if (vastus.length() < 500) { // kui vastus on alla teatud lubatud suuruse
                            sender.sendMsgWithSenderToChannel("Main", client, vastus);
                        } else {
                            String tooLongMsg = "The message you have entered contains too many characters. Please keep it under 500 characters.";
                            sender.sendText(tooLongMsg, socketChannel);
                        }
                    } else { // kasutaja pole sisse logitud
                        String loginRequest = "Please log in or create an account before sending messages with /register [username] [password]!";
                        sender.sendText(loginRequest, socketChannel);
                    }
                } catch (StringIndexOutOfBoundsException e) { // See exception visatakse, kui command oli /exit ja ühendus sulgetakse enne sõne töötlemist.
                    Server.logger.info(String.format("Client %s closed client-side connection.", client.getName()));
                    client.setLoggedIn(false);
                    this.socketClientMap.remove(socketChannel);
                    socketChannel.close();
                    key.cancel();
                }
            }
        } catch (IOException e) {
            client.setLoggedIn(false);
            this.socketClientMap.remove(socketChannel);
            Server.logger.info("Connection closed by client: " + s.getRemoteSocketAddress());
            socketChannel.close();
            key.cancel();
        }
    }

    private void setClientType(SocketChannel socketChannel, String vastus) throws IOException {
        ClientInfo client = socketClientMap.get(socketChannel);
        if (vastus.startsWith("GET / HTTP/1.1")){
            client.setClientType(ClientType.WEBSOCKET);
            acceptWebsocket(socketChannel, vastus);
            Server.logger.info("Javascript (ws) client connected.");
        } else {
            client.setClientType(ClientType.JAVA);
            Server.logger.info("Java client connected.");
        }
    }

    private void acceptWebsocket(SocketChannel socketChannel, String vastus) throws IOException {
        Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(vastus); //loeb vastusest krüpto võtme.
        try {
            match.find();
            byte[] wsVastus = ("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Sec-WebSocket-Accept: "
                    + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)))
                    + "\r\n\r\n").getBytes();
            ByteBuffer bb = ByteBuffer.wrap(wsVastus);
            socketChannel.write(bb);
        } catch (NoSuchAlgorithmException e) {
            Server.logger.warning(e.getMessage());
        }

    }

    /**
     * Dekodeerib Websocket standardile vastava baidijada selliseks, mida saaks ka mujal kasutada.
     * @param buffer WS ühendusest saadud ByteBuffer.
     * @return dekodeeritud byte[] massiiv, mille sisuks on ainult saadetud sõnum.
     */

    private byte[] decode(ByteBuffer buffer){ //WS byte magic võetud https://gist.github.com/hide1202/3ae71fd794ac76aa72c456f632e12c5f
        byte firstByte = buffer.get();
        byte secondByte = buffer.get();

        byte mask = (byte) ((secondByte & 128) >> 7);
        long length = (secondByte & 64) | (secondByte & 32) | (secondByte & 16) | (secondByte & 8) | (secondByte & 4) | (secondByte & 2) | (secondByte & 1);

        if (length == 126) {
            length = buffer.getShort();
        } else if (length == 127) {
            length = buffer.getLong();
        }

        byte[] dataBytes = new byte[(int) length];

        byte[] maskValue;
        if (mask == 1) {
            maskValue = new byte[4];
            buffer.get(maskValue);

            for (int i = 0; i < length; i++) {
                byte data = buffer.get();
                dataBytes[i] = (byte) (data ^ maskValue[i % 4]);
            }
        }
        return dataBytes;
    }
}