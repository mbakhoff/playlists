package red.sigil.playlists.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import red.sigil.playlists.entities.Account;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.services.PlaylistService.PlaylistItemChange;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class PlaylistNotificationService {

  private static final Logger log = LoggerFactory.getLogger(PlaylistNotificationService.class);

  private final JavaMailSender mailSender;
  private final SpringTemplateEngine engine;

  public PlaylistNotificationService(JavaMailSender mailSender, SpringTemplateEngine engine) {
    this.mailSender = mailSender;
    this.engine = engine;
  }

  public void sendChangeNotification(Account account, Map<Playlist, List<PlaylistItemChange>> changes) {
    String sendTo = account.getEmail();
    try {
      log.info("sending notification to " + sendTo + " with " + changes.size() + " playlists");
      sendHtml(sendTo, generateNotification(changes));
    } catch (Exception e) {
      log.error("failed to send notification to " + sendTo, e);
    }
  }

  private void sendHtml(String recipientEmail, String message) throws Exception {
    MimeMessage msg = mailSender.createMimeMessage();
    msg.setRecipients(Message.RecipientType.TO, recipientEmail);
    msg.setSubject("Youtube tracks changed");
    msg.setText(message, "utf-8", "html");
    msg.setSentDate(new Date());
    mailSender.send(msg);
  }

  String generateNotification(Map<Playlist, List<PlaylistItemChange>> playlistChanges) throws IOException {
    Context model = new Context();
    model.setVariable("playlistChanges", playlistChanges);
    return engine.process("notification", model);
  }
}
