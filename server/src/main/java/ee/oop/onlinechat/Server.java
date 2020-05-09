package ee.oop.onlinechat;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class Server
{
    private static boolean running = true;
    protected final static Logger logger = Logger.getLogger(ServerLog.class.getName());

    public static void main(String[] args) {
        ServerLog.setup();

        try {
            new Ühendus("localhost", 1337).ühenda();
        } catch (IOException e) {
            logger.severe("Error in the main method of the server: " + e.getMessage());
        }
    }

    /**
     * Sets the running flag to false so all loops using it as a refrence will exit gracefully.
     * If the server has not shut down in 5 seconds, forcefully closes it.
     */
    public static void shutdown(){
        logger.warning("Server started shutdown procedure...");
        running = false;
        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
            for(Handler h:logger.getHandlers()) //Suleb logifailid korralikult.. kui log kaustas on .lck failid siis seda ei juhtunud...
            {
                h.close();
            }
            System.exit(0);

        }).start();
    }
    public static boolean isRunning() {
        return running;
    }
}
