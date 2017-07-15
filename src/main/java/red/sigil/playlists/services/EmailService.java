package red.sigil.playlists.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.util.Date;

@Component
public class EmailService {

  private final JavaMailSender mailSender;

  @Autowired
  public EmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void sendHtml(String recipientEmail, String title, String message) throws Exception {
    MimeMessage msg = mailSender.createMimeMessage();
    msg.setRecipients(Message.RecipientType.TO, recipientEmail);
    msg.setSubject(title);
    msg.setText(message, "utf-8", "html");
    msg.setSentDate(new Date());
    mailSender.send(msg);
  }
}
