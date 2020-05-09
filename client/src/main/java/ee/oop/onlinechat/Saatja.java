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
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        sendMessage("JAVA/Hello server"); //Esimese asjana saadab serverile sõnumi, mis ütleb talle mis tüüpi kliendiga on tegu.
        Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
        String message = in.nextLine();
        while (!message.equals("/exit")) {
            sendMessage(message);
            message = in.nextLine();
        }
        try {
            this.bufferedWriter.close();
        } catch (IOException e) {
            System.out.println("Error shutting down the client.");
            System.out.println(e.getMessage());
        }
    }
    public void sendMessage(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.flush();
        } catch (IOException e) {
            System.out.println("Error writing: ");
            System.out.println(e.getMessage());
        }
    }
}
