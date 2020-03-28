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
     * If the server has not shut down in 5 seconds, forcefully closes it.
     */
    public static void shutdown(){
        System.out.println("Shutting down server...");
        running = false;
        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
            System.exit(0);

        }).start();
    }
    public static boolean isRunning() {
        return running;
    }
}
