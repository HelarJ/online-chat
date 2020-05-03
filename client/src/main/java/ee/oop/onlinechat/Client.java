package ee.oop.onlinechat;

import java.io.IOException;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws IOException, InterruptedException {
        Socket client = new Socket("localhost", 1337);
        System.out.println("Client started...");

        final Kuulaja kuulaja = new Kuulaja(client);
        Thread.sleep(50);
        final Saatja saatja = new Saatja(client);

        new Thread(kuulaja).start();
        new Thread(saatja).start();
    }
}
