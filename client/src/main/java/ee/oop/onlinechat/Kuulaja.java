package ee.oop.onlinechat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Kuulaja implements Runnable {
    private BufferedReader bufferedReader;

    public Kuulaja(Socket socket) throws IOException {
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        System.out.println("Started listening...");
        while(true){
            String output;
            try {
                while ((output = bufferedReader.readLine())!=null){
                    System.out.println(output);
                }

            } catch (IOException e) {
                System.out.println("Connection is closed.");
                break;
            }

        }
        System.out.println("Closed client.");
        System.exit(1);
    }
}
