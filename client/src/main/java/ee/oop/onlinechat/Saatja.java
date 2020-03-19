package ee.oop.onlinechat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Saatja implements Runnable {
    SocketChannel client;

    public Saatja(SocketChannel client) {
        this.client = client;
    }

    @Override
    public void run() {
        System.out.println("Waiting for input...");
        Scanner in = new Scanner(System.in);
        String message = in.nextLine();
        while (!message.equals("/exit")) {

            byte[] mBytes = message.getBytes();
            ByteBuffer byteBuffer = ByteBuffer.wrap(mBytes);
            try {
                client.write(byteBuffer);
            } catch (IOException e) {
                System.out.println("Error writing: ");
                System.out.println(e.getMessage());
            }
            byteBuffer.clear();
            message = in.nextLine();
        }
        try {
            client.close();
        } catch (IOException e) {
            System.out.println("Couldn't close the client.");
            System.out.println(e.getMessage());
        }


    }
}
