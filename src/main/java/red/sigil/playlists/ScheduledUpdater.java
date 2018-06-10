package red.sigil.playlists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import red.sigil.playlists.model.Account;
import red.sigil.playlists.model.Playlist;
import red.sigil.playlists.model.PlaylistItemChange;
import red.sigil.playlists.services.PlaylistNotificationService;
import red.sigil.playlists.services.PlaylistService;

import java.util.List;
import java.util.Map;

@Component
@Transactional(rollbackFor = Throwable.class)
public class ScheduledUpdater {

  private static final Logger log = LoggerFactory.getLogger(ScheduledUpdater.class);

  private final PlaylistService playlistService;
  private final PlaylistNotificationService notificationService;

  public ScheduledUpdater(PlaylistService playlistService, PlaylistNotificationService notificationService) {
    this.playlistService = playlistService;
    this.notificationService = notificationService;
  }

  @Scheduled(fixedDelay = 60_000, initialDelay = 3000)
  public void synchronize() throws Exception {
    Map<Account, Map<Playlist, List<PlaylistItemChange>>> changesByAccount = playlistService.update();
    changesByAccount.forEach((account, changes) -> {
      try {
        log.info("sending notification to " + account.getEmail() + " with " + changes.size() + " playlists");
        notificationService.sendChangeNotification(account, changes);
      } catch (Exception e) {
        log.error("failed to send notification to " + account.getEmail(), e);
      }
    });
  }

  @Scheduled(fixedDelay = 600_000, initialDelay = 10_000)
  public void cleanup() {
    playlistService.removeOrphans();
  }
}
