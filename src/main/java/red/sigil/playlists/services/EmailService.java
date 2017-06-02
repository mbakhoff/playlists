package red.sigil.playlists.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import red.sigil.playlists.services.PropertyService;

import javax.annotation.PostConstruct;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

@Component
public class EmailService {

  @Autowired
  private PropertyService propertyService;

  private Session session;
  private String from;

  @PostConstruct
  private void prepareSession() {
    Properties props = propertyService.getProps();
    this.from = props.getProperty("mail.from");
    this.session = Session.getInstance(props, new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(
            props.getProperty("mail.smtps.user"),
            props.getProperty("mail.smtps.pass"));
      }
    });
  }

  public void send(String recipientEmail, String title, String message) throws Exception {
    MimeMessage msg = new MimeMessage(session);
    msg.setFrom(from);
    msg.setRecipients(Message.RecipientType.TO, recipientEmail);
    msg.setSubject(title);
    msg.setText(message, "utf-8", "html");
    msg.setSentDate(new Date());
    Transport.send(msg);
  }
}
