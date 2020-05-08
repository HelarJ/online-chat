package ee.oop.onlinechat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class ServerLog {
    private final static Logger logger = Logger.getLogger(ServerLog.class.getName());

    public static void setup() {
        File file = new File("log"); //Kui working directorys(kui ideas tööe paned siis projekti kaust) pole log kausta siis loob selle.
        if (!file.exists()){
            file.mkdir();
        }
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        FileHandler fh;
        FileHandler fh2;
        //System.out.println(file.getAbsolutePath());
        try {

            fh = new FileHandler(file.getAbsolutePath()+"/server_"+dateFormat.format(date)+".log"); //Tähtsamaid sõnumeid salvestatakse XML formaadis ning eraldi faili.
            fh.publish(new LogRecord(Level.INFO, "Start of the server log."));
            fh.flush();
            fh.setLevel(Level.FINEST);

            fh2 = new FileHandler(file.getAbsolutePath()+"/output_"+dateFormat.format(date)+".log"); //Kõikide sõnumite logger läheb läbi simpleformatteri, mis teeb selle loetavamaks.
            fh2.setFormatter(new SimpleFormatter());
            fh2.publish(new LogRecord(Level.INFO, "Start of the output log."));
            fh2.flush();
            fh2.setLevel(Level.INFO);

        } catch (IOException e){
            logger.severe("Error creating the log file.");
            System.out.println(e.getMessage());
            throw new RuntimeException("Error creating the log file.");
        }


        logger.addHandler(fh);
        logger.addHandler(fh2);
    }
}
