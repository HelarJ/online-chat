package ee.oop.onlinechat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class ServerLog {
    private final static Logger logger = Logger.getLogger(ServerLog.class.getName());

    public static void setup() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        FileHandler fh;
        FileHandler fh2;
        try {

            fh = new FileHandler("log/server_"+dateFormat.format(date)+".log"); //Tähtsamaid sõnumeid salvestatakse XML formaadis ning eraldi faili.
            fh.publish(new LogRecord(Level.INFO, "Start of the server log."));
            fh.flush();
            fh.setLevel(Level.FINEST);

            fh2 = new FileHandler("log/output_"+dateFormat.format(date)+".log"); //Kõikide sõnumite logger läheb läbi simpleformatteri, mis teeb selle loetavamaks.
            fh2.setFormatter(new SimpleFormatter());
            fh2.publish(new LogRecord(Level.INFO, "Start of the output log."));
            fh2.flush();
            fh2.setLevel(Level.INFO);

        } catch (IOException e){
            logger.severe("Error creating the log file.");
            throw new RuntimeException("Error creating the log file.");
        }


        logger.addHandler(fh);
        logger.addHandler(fh2);
    }
}
