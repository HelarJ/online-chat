package ee.oop.onlinechat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.*;

public class ServerLog {
    private final static Logger logger = Logger.getLogger(ServerLog.class.getName());
    private static boolean emailErrorLogging = false;

    public static void setup() {
        Properties prop = new Properties();
        try (InputStream input = Mailer.class.getClassLoader().getResourceAsStream("config.properties")) {
            assert input != null;
            prop.load(input);
            emailErrorLogging = Boolean.parseBoolean(prop.getProperty("mail.errorlogging"));
        } catch (IOException e) {
            Server.logger.warning("Error loading properties");
        }

        File file = new File("log"); //Kui working directorys(kui ideas tööle paned siis projekti kaust) pole log kausta siis loob selle.
        if (!file.exists()){
            if(file.mkdir()){
                Server.logger.info("Log directory created successfully");
            }
        }
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        if (emailErrorLogging){ //kui config.properties failis on see true siis saadetakse adminile email, kui serveril tekib SEVERE tasemega error.
            Handler eh = new EmailErrorHandler((String) prop.get("mail.adminemail"));
            eh.setLevel(Level.SEVERE);
            eh.setFormatter(new XMLFormatter());
            logger.addHandler(eh);
            logger.info("email logging enabled!");
        }

        FileHandler fh;
        try {

            fh = new FileHandler(file.getAbsolutePath()+"/output_"+dateFormat.format(date)+".log");
            //Kõikide sõnumite logger läheb läbi simpleformatteri, mis teeb selle loetavamaks.
            fh.setFormatter(new SimpleFormatter());
            fh.publish(new LogRecord(Level.INFO, "Start of the output log."));
            fh.flush();
            fh.setLevel(Level.INFO);


        } catch (IOException e){
            logger.warning("Error creating the log file."+e.getMessage());
            throw new RuntimeException("Error creating the log file.");
        }
        logger.addHandler(fh);
    }
}
