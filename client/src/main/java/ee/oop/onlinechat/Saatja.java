package ee.oop.onlinechat;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Saatja implements Runnable {
    Socket socket;
    BufferedWriter bufferedWriter;

    public Saatja(Socket socket) throws IOException {
        this.socket = socket;
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        System.out.println("Waiting for input...");
        Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
        String message = in.nextLine();
        while (!message.equals("/exit")) {
            try {
                bufferedWriter.write(message);
                bufferedWriter.flush();
            } catch (IOException e) {
                System.out.println("Error writing: ");
                System.out.println(e.getMessage());
            }
            message = in.nextLine();
        }
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Couldn't close the client.");
            System.out.println(e.getMessage());
        }


    }
}
