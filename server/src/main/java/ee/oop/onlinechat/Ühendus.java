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
    private Map<SocketChannel, String> dataMapper;
    private InetSocketAddress listenAddress;

    public Ühendus(String aadress, int port)  {
        listenAddress = new InetSocketAddress(aadress, port);
        dataMapper = new HashMap<>();
    }


    public void ühenda() throws IOException {
        this.selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        serverChannel.socket().bind(listenAddress);
        serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started...");
        while (true) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
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

        dataMapper.put(channel, "Default"); //jätab channeli meelde koos default nimega.
        channel.register(this.selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        Socket s = channel.socket();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int count;
        count = channel.read(buffer);

        byte[] data = new byte[count];
        System.arraycopy(buffer.array(), 0, data, 0, count);
        String vastus = new String(data);
        System.out.println("Got: " + vastus);

        if (vastus.equals("/exit")){
            this.dataMapper.remove(channel);
            System.out.printf("Connection closed by client: %s%n", s.getRemoteSocketAddress());
            channel.close();
            key.cancel();
            return;
        }
        sendToAll(ByteBuffer.wrap(data)); //saadab kõigile channelitele
    }
    private void sendToAll(ByteBuffer data){
            dataMapper.forEach((c, d) -> { //saadab igale ühendusele, mis on kaardistatud dataMapperis.
                try {
                    c.write(data);
                    System.out.printf("Sent: %s to %s%n", new String(data.array()), c.getRemoteAddress());
                    data.rewind();
                } catch (IOException e){
                    System.out.println("Error sending to channel: "+ e.getMessage());
                }
            });
    }
}