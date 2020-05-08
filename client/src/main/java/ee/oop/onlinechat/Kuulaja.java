package ee.oop.onlinechat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import ee.ut.oop.Message;

public class Kuulaja implements Runnable {
    private BufferedReader bufferedReader;

    public Kuulaja(Socket socket) throws IOException {
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        System.out.println("Started listening...");
        Gson gson = new Gson();
        while(true){
            String output;
            try {
                while ((output = bufferedReader.readLine())!=null){ //Et see toimiks PEAB serveri poolt tulev sõnum lõppema uue rea sümbolitega.
                    Message msg = gson.fromJson(output, Message.class);
                    System.out.println(msg.toString());
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
