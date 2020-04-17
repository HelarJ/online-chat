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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

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
        String greeting = "Welcome to blablaChat! To start chatting, please /register [username] [password] or /login [username] [password]!";
        socketClientMap.put(channel, new ClientInfo()); //jätab alguses channeli meelde koos default nimega.
        channel.register(this.selector, SelectionKey.OP_READ);
        sender.sendTextBack(greeting, channel);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
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
            Server.logger.info("Got: " + vastus + " from " + socketChannel.getRemoteAddress());
            try {
                if (vastus.substring(0, 1).equals("/")) { // kui oli command
                    CommandHandler commandHandler = new CommandHandler(socketClientMap.get(socketChannel), socketChannel, sender); //handler saab clientinfo, socketChanneli ja sender objekti mida kasutada.
                    commandHandler.handleCommand(vastus);
                } else if (socketClientMap.get(socketChannel).isLoggedIn()) { // kasutaja on sisse logitud
                    if (vastus.length() < 256) { // kui vastus on alla teatud lubatud suuruse
                        sender.sendMsgWithSenderToChannel("Main", socketClientMap.get(socketChannel), vastus);
                    } else {
                        String tooLongMsg = "The message you have entered contains too many characters. Please keep it under 256 characters.";
                        sender.sendTextBack(tooLongMsg, socketChannel);
                    }
                } else { // kasutaja pole sisse logitud
                    String loginRequest = "Please log in or create an account before sending messages with /register [username] [password]!";
                    sender.sendTextBack(loginRequest, socketChannel);
                }
            } catch (StringIndexOutOfBoundsException e) { // See exception visatakse, kui command oli /exit ja ühendus sulgetakse enne sõne töötlemist.
                Server.logger.info(String.format("Client %s closed client-side connection.", socketClientMap.get(socketChannel).getName()));
                socketClientMap.get(socketChannel).setLoggedIn(false);
                this.socketClientMap.remove(socketChannel);
                socketChannel.close();
                key.cancel();
            }
        } catch (IOException e) {
            socketClientMap.get(socketChannel).setLoggedIn(false);
            this.socketClientMap.remove(socketChannel);
            Server.logger.info("Connection closed by client: " + s.getRemoteSocketAddress());
            socketChannel.close();
            key.cancel();
        }
    }


}