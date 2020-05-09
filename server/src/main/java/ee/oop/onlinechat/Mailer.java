package ee.oop.onlinechat;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Mailer {
    private Properties prop;

    public Mailer() {
        this.prop = new Properties();
        try (InputStream input = Mailer.class.getClassLoader().getResourceAsStream("config.properties")) {
            assert input != null;
            prop.load(input);

        } catch (IOException e) {
            Server.logger.warning("Error loading properties");
        }
    }

    public void sendEmail(String title, String message, String to){
        Session session;
        try {
            session = Session.getInstance(prop, new javax.mail.Authenticator() {

                protected PasswordAuthentication getPasswordAuthentication() {

                    return new PasswordAuthentication((String) prop.get("mail.username"), (String) prop.get("mail.password"));

                }
            });
        } catch (NullPointerException e) {
            Server.logger.warning("Error reading data from config file: "+e.getMessage());
            return;
        }
        session.setDebug(false);

        try {
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom("OnlineChat <"+ prop.get("mail.username") +">");
            mimeMessage.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
            mimeMessage.setSubject(title);
            mimeMessage.setText(message);
            Transport.send(mimeMessage);
            Server.logger.info("Email sent successfully!");

        } catch (MessagingException e){
            Server.logger.warning("Error sending email: "+e.getMessage());
        }



    }
}
