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
import java.util.*;

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
        String greeting = "Welcome to blablaChat! To start chatting, please /register [username] [password] or /login [username] [password]!";
        socketClientMap.put(channel, new ClientInfo()); //jätab alguses channeli meelde koos default nimega.
        channel.register(this.selector, SelectionKey.OP_READ);
        sender.sendTextBack(greeting, channel);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        Socket s = socketChannel.socket();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int count;
        byte[] data;
        try {
            count = socketChannel.read(buffer);
            data = new byte[count];
            System.arraycopy(buffer.array(), 0, data, 0, count);
            String vastus = new String(data);
            System.out.println("Got: " + vastus);

            if (vastus.substring(0, 1).equals("/")) { // kui oli command
                CommandHandler commandHandler = new CommandHandler(socketClientMap.get(socketChannel), socketChannel, sender); //handler saab clientinfo, socketChanneli ja sender objekti mida kasutada.
                commandHandler.handleCommand(vastus);
            } else if (socketClientMap.get(socketChannel).isLoggedIn()) { // kasutaja on sisse logitud
                sender.sendMsgWithSenderToChannel("Main", socketClientMap.get(socketChannel), vastus);
            } else { // kasutaja pole sisse logitud
                String loginRequest = "Please log in or create an account before sending messages with /register [username] [password]!";
                sender.sendTextBack(loginRequest, socketChannel);
            }
        } catch (IOException e) {
            socketClientMap.get(socketChannel).setLoggedIn(false);
            this.socketClientMap.remove(socketChannel);
            System.out.printf("Connection closed by client: %s%n", s.getRemoteSocketAddress());
            socketChannel.close();
            key.cancel();
        }
    }



}