package ee.oop.onlinechat;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.google.gson.Gson;
import ee.ut.oop.Crypto;
import ee.ut.oop.Message;

public class Kuulaja implements Runnable {
    private Crypto decrypter;
    SocketChannel socketChannel;

    public Kuulaja(SocketChannel socketChannel, Crypto decrypter){
        this.socketChannel = socketChannel;
        this.decrypter = decrypter;
    }

    @Override
    public void run() {
        Gson gson = new Gson();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while(true){
            try {
                int count;
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                try {
                    count = socketChannel.read(buffer);
                    byte[] data = new byte[count];
                    System.arraycopy(buffer.array(), 0, data, 0, count);
                    bos.writeBytes(data);
                    buffer.clear();
                    String output = new String(decrypter.decrypt(bos.toByteArray()), StandardCharsets.UTF_8);
                    Message msg = gson.fromJson(output, Message.class);
                    System.out.println(msg.toString());
                } catch (NullPointerException e){
                    System.out.println("Error decrypting.");
                }
                bos.reset();

            } catch (IOException e) {
                System.out.println("Connection is closed.");
                break;
            } finally {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        try {
            this.socketChannel.close();
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Error shutting down the client: " + e.getMessage());
        }
    }
}
