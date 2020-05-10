package ee.oop.onlinechat;

import ee.ut.oop.Crypto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Saatja implements Runnable {
    SocketChannel socketChannel;
    Crypto encrypter;

    public Saatja(SocketChannel socketChannel, Crypto encrypter) {
        this.socketChannel = socketChannel;
        this.encrypter = encrypter;
    }

    @Override
    public void run() {
        Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
        String message = in.nextLine();
        while (!message.equals("/exit")) {
            sendMessage(message);
            message = in.nextLine();
        }
        try {
            socketChannel.close();
        } catch (IOException e) {
            System.out.println("Error shutting down the client.");
            System.out.println(e.getMessage());
        }
    }
    public void sendMessage(String message) {
        try {
            socketChannel.write(ByteBuffer.wrap(encrypter.encrypt(message.getBytes(StandardCharsets.UTF_8))));
        } catch (IOException e) {
            System.out.println("Error writing: ");
            System.out.println(e.getMessage());
        }
    }
}
