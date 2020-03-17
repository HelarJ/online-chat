package ee.oop.onlinechat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class Client {
    public static void main(String[] args) throws IOException {
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 1337);
        SocketChannel client = SocketChannel.open(hostAddress);
        System.out.println("Client started...");

        final Kuulaja kuulaja = new Kuulaja(client);
        final Saatja saatja = new Saatja(client);

        new Thread(kuulaja).start();
        new Thread(saatja).start();



    }
}
