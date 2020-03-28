package ee.oop.onlinechat;

import java.io.IOException;

public class Server
{
    private static boolean running = true;

    public static void main(String[] args) {

        try {
            new Ühendus("localhost", 1337).ühenda();
        } catch (IOException e) {
            System.out.println("ERROR");
            System.out.println(e.getMessage());
        }
    }

    /**
     * Sets the running flag to false so all loops using it as a refrence will exit gracefully.
     */
    public static void shutdown(){
        running = false;
    }
    public static boolean isRunning() {
        return running;
    }
}
