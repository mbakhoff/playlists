package red.sigil.playlists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import red.sigil.playlists.entities.Account;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.services.AccountRepository;
import red.sigil.playlists.services.PlaylistNotificationService;
import red.sigil.playlists.services.PlaylistRepository;
import red.sigil.playlists.services.PlaylistService;
import red.sigil.playlists.services.PlaylistService.PlaylistItemChange;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScheduledUpdater {

  private static final Logger log = LoggerFactory.getLogger(ScheduledUpdater.class);

  private final AccountRepository accountRepository;
  private final PlaylistRepository playlistRepository;
  private final PlaylistService playlistService;
  private final PlaylistNotificationService notificationService;

  @Autowired
  public ScheduledUpdater(AccountRepository accountRepository, PlaylistRepository playlistRepository, PlaylistService playlistService, PlaylistNotificationService notificationService) {
    this.accountRepository = accountRepository;
    this.playlistRepository = playlistRepository;
    this.playlistService = playlistService;
    this.notificationService = notificationService;
  }

  @Transactional
  @Scheduled(fixedDelay = 60_000, initialDelay = 3000)
  public void runSyncTasks() throws Exception {
    Map<Playlist, List<PlaylistItemChange>> changes = findRecentPlaylistChanges();
    Map<Account, Map<Playlist, List<PlaylistItemChange>>> changesByAccount = getChangesByAccount(changes);
    changesByAccount.forEach(notificationService::sendChangeNotification);
  }

  private Map<Playlist, List<PlaylistItemChange>> findRecentPlaylistChanges() throws Exception {
    Map<Playlist, List<PlaylistItemChange>> playlistChanges = new HashMap<>();
    for (Playlist playlist : playlistRepository.findAllByOrderByLastUpdateAsc(30)) {
      if (playlist.getLastUpdate() != null && playlist.getLastUpdate().plus(1, ChronoUnit.HOURS).isAfter(Instant.now()))
        continue;

      List<PlaylistItemChange> changes = playlistService.findPlaylistChanges(playlist);
      changes.removeIf(c -> c.oldTitle == null);
      if (!changes.isEmpty())
        playlistChanges.put(playlist, changes);
    }
    return playlistChanges;
  }

  private Map<Account, Map<Playlist, List<PlaylistItemChange>>> getChangesByAccount(Map<Playlist, List<PlaylistItemChange>> allChanges) {
    Map<Account, Map<Playlist, List<PlaylistItemChange>>> result = new HashMap<>();
    allChanges.forEach((playlist, changes) -> {
      for (Account account : accountRepository.findByPlaylist(playlist.getId())) {
        result.computeIfAbsent(account, s -> new HashMap<>()).put(playlist, changes);
      }
    });
    return result;
  }
}
