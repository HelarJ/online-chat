package ee.oop.onlinechat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Kuulaja implements Runnable {
    private SocketChannel client;

    public Kuulaja(SocketChannel client) {
        this.client = client;
    }

    @Override
    public void run() {
        System.out.println("Started listening...");
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        while(true){
            int count;
            try {
                count = client.read(byteBuffer);

            } catch (IOException e) {
                System.out.println("Connection is closed.");
                break;
            }
            byte[] data = new byte[count];
            System.arraycopy(byteBuffer.array(), 0, data, 0, count); //loob buffrist arv suurusega array
            System.out.println("Got: " + new String(data));
            byteBuffer.clear();
        }
        System.out.println("Closed client.");
    }
}
