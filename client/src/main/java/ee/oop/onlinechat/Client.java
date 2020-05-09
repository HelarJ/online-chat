package ee.oop.onlinechat;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws IOException, InterruptedException {
        int aeg = 5000;
        Socket client;
        while (true){
            try {
                client = new Socket("localhost", 1337);
                break;
            } catch (ConnectException e) {
                System.out.println("Error connecting to the server.. Retrying in " + aeg / 1000+"s.");
                Thread.sleep(aeg);
                aeg += 1000;

                if (aeg > 20000) {
                    aeg = 1000;
                }
            }
        }



        System.out.println("Client connected!");

        final Kuulaja kuulaja = new Kuulaja(client);
        Thread.sleep(50);
        final Saatja saatja = new Saatja(client);

        Thread kuulajaThread = new Thread(kuulaja);
        Thread saatjaThread = new Thread(saatja);
        kuulajaThread.start();
        saatjaThread.start();

        kuulajaThread.join(); //need joinid toimuvad alles siis, kui while loopid nendes threadides otsa saavad.
        saatjaThread.join();
        client.close();

        System.out.println("Client shut down.");
    }
}
