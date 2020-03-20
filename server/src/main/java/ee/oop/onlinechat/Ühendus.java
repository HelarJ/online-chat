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
    private Map<SocketChannel, KliendiInfo> dataMapper;
    private InetSocketAddress listenAddress;

    public Ühendus(String aadress, int port) {
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
        Scanner in = new Scanner(System.in); // lisasin temporarily scanneri closimiseks, et loop ei annaks warningut
        do {
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
        } while (!in.nextLine().equals(""));
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        System.out.println("Connected to: " + remoteAddr);
        byte[] greeting = "Welcome to blablaChat! To change your name, type /name [name here]!".getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(greeting);
        dataMapper.put(channel, new KliendiInfo()); //jätab alguses channeli meelde koos default nimega.
        channel.register(this.selector, SelectionKey.OP_READ);

        sendBack(buffer, channel);
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
                String[] vastuseTükid = vastus.split(" ");
                switch (vastuseTükid[0]) {
                    case "/help":
                        byte[] commandid = "Available commands: /help, /name, /exit".getBytes();
                        buffer = ByteBuffer.wrap(commandid);
                        sendBack(buffer, channel);
                        break;

                    case "/name":
                        StringBuilder nimi = new StringBuilder();
                        for (int i = 1; i < vastuseTükid.length; i++) {
                            nimi.append(vastuseTükid[i]);
                            if (!(i == vastuseTükid.length - 1)) {
                                nimi.append(" ");
                            }
                        }
                        dataMapper.get(channel).setNimi(nimi.toString());
                        byte[] nameChanged = ("Name successfully changed to " + nimi + ".").getBytes();
                        buffer = ByteBuffer.wrap(nameChanged);
                        sendBack(buffer, channel);
                        break;
                }
            } else {
                sendTextToAll(dataMapper.get(channel), vastus); //saadab kõigile channelitele todo (võib-olla peab panema tagasi try seest välja ja lisama IF command checki (et ei saadaks kõigile kirjutatud commandi))
            }
        } catch (IOException e) {
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

    private void sendTextToAll(KliendiInfo saatja, String tekst) { // sama asi mis sendToAll, aga selle asemel et saadab byteBufferi siis saadab stringi
        String saadetavTekst = saatja.getNimi() + ": " + tekst;
        byte[] bytes = saadetavTekst.getBytes();
        ByteBuffer data = ByteBuffer.wrap(bytes);
        dataMapper.forEach((c, d) -> { //saadab igale ühendusele, mis on kaardistatud dataMapperis.
            try {
                c.write(data);
                System.out.printf("Sent: %s to %s%n at %s%n", new String(data.array()), d.getNimi(), c.getRemoteAddress());
                data.rewind();
            } catch (IOException e) {
                System.out.println("Error sending to channel: " + e.getMessage());
            }
        });
    }

    private void sendBack(ByteBuffer data, SocketChannel c) {
        try {
            c.write(data);
            System.out.printf("Sent: %s to %s%n at %s%n", new String(data.array()), dataMapper.get(c).getNimi(), c.getRemoteAddress());
            data.rewind();
        } catch (IOException e) {
            System.out.println("Error sending to channel: " + e.getMessage());
        }
    }
}