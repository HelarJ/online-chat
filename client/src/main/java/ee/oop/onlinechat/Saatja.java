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
        String message;
        while (true){
            message = in.nextLine();
            byte[] mBytes= message.getBytes();
            ByteBuffer byteBuffer = ByteBuffer.wrap(mBytes);
            try{
                client.write(byteBuffer);
            } catch (IOException e){
                System.out.println("Error writing: ");
                System.out.println(e.getMessage());
            }

            System.out.println("Sent: "+message);
            byteBuffer.clear();
            if (message.equals("/exit")){
                break;
            }
        }
        try {
            client.close();
        } catch (IOException e) {
            System.out.println("Couldnt close the client.");
            System.out.println(e.getMessage());
        }


    }
}
