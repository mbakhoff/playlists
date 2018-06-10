package red.sigil.playlists.services;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import red.sigil.playlists.model.Account;
import red.sigil.playlists.model.Playlist;
import red.sigil.playlists.model.PlaylistItemChange;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class PlaylistNotificationService {

  private final JavaMailSender mailSender;
  private final SpringTemplateEngine engine;

  public PlaylistNotificationService(JavaMailSender mailSender, SpringTemplateEngine engine) {
    this.mailSender = mailSender;
    this.engine = engine;
  }

  public void sendChangeNotification(Account account, Map<Playlist, List<PlaylistItemChange>> changes) throws Exception {
    sendHtml(account.getEmail(), generateNotification(changes));
  }

  private void sendHtml(String recipientEmail, String message) throws Exception {
    MimeMessage msg = mailSender.createMimeMessage();
    msg.setRecipients(Message.RecipientType.TO, recipientEmail);
    msg.setSubject("Youtube tracks changed");
    msg.setText(message, "utf-8", "html");
    msg.setSentDate(new Date());
    mailSender.send(msg);
  }

  private String generateNotification(Map<Playlist, List<PlaylistItemChange>> playlistChanges) throws IOException {
    Context model = new Context();
    model.setVariable("playlistChanges", playlistChanges);
    return engine.process("notification", model);
  }
}
