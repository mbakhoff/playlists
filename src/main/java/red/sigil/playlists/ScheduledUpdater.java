package red.sigil.playlists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import red.sigil.playlists.entities.Account;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.entities.PlaylistItem;
import red.sigil.playlists.services.EmailService;
import red.sigil.playlists.services.FreemarkerEmailFormatter;
import red.sigil.playlists.services.PlaylistFetchService;
import red.sigil.playlists.services.PlaylistFetchService.ItemInfo;
import red.sigil.playlists.services.PlaylistRepository;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ScheduledUpdater {

  private static final Logger log = LoggerFactory.getLogger(ScheduledUpdater.class);

  private final PlaylistRepository playlistRepository;
  private final PlaylistFetchService playlistFetchService;
  private final EmailService emailService;
  private final FreemarkerEmailFormatter emailFormatter;

  @Autowired
  public ScheduledUpdater(PlaylistRepository playlistRepository, PlaylistFetchService playlistFetchService, EmailService emailService, FreemarkerEmailFormatter emailFormatter) {
    this.playlistRepository = playlistRepository;
    this.playlistFetchService = playlistFetchService;
    this.emailService = emailService;
    this.emailFormatter = emailFormatter;
  }

  @Transactional
  @Scheduled(fixedDelay = 60_000, initialDelay = 3000)
  public void runSyncTasks() throws Exception {
    List<PlaylistItemChange> changes = findChangedPlaylistItems();
    if (!changes.isEmpty()) {
      Map<Account, List<PlaylistChange>> changesByAccount = getChangesByAccount(changes);
      for (Map.Entry<Account, List<PlaylistChange>> entry : changesByAccount.entrySet()) {
        String recipientEmail = entry.getKey().getEmail();
        try {
          log.info("sending notification to " + recipientEmail + " with " + entry.getValue().size() + " playlists");
          String message = emailFormatter.generateNotificationMessage(entry.getValue());
          emailService.send(recipientEmail, "Youtube tracks changed", message);
        } catch (Exception e) {
          log.error("failed to send notification to " + recipientEmail, e);
        }
      }
    }
  }

  private List<PlaylistItemChange> findChangedPlaylistItems() throws Exception {
    List<PlaylistItemChange> changes = new ArrayList<>();
    for (Playlist playlist : playlistRepository.findAllByOrderByLastUpdateDesc(new PageRequest(0, 30))) {
      if (playlist.getLastUpdate().plus(1, ChronoUnit.HOURS).isAfter(Instant.now()))
        continue;

      ItemInfo info = playlistFetchService.read(playlist.getYoutubeId());
      if (info == null) {
        playlistRepository.delete(playlist);
        log.info("deleting playlist " + playlist.getYoutubeId());
        continue;
      }

      log.info("updating playlist " + playlist.getYoutubeId());
      Set<PlaylistItem> oldItems = playlist.getPlaylistItems();
      Set<PlaylistItem> newItems;
      try {
        newItems = toSet(playlistFetchService.readItems(playlist.getYoutubeId()));
      } catch (Exception e) {
        log.error("failed to update " + playlist.getYoutubeId(), e);
        continue;
      }

      Map<String, PlaylistItem> mappedNew = new HashMap<>();
      for (PlaylistItem newItem : newItems)
        mappedNew.put(newItem.getYoutubeId(), newItem);

      Map<String, PlaylistItem> mappedOld = new HashMap<>();
      for (PlaylistItem oldItem : oldItems)
        mappedOld.put(oldItem.getYoutubeId(), oldItem);

      for (PlaylistItem oldItem : new ArrayList<>(oldItems)) {
        PlaylistItem newItem = mappedNew.get(oldItem.getYoutubeId());
        if (newItem == null) {
          changes.add(new PlaylistItemChange(playlist, oldItem.getYoutubeId(), oldItem.getTitle(), null));
          playlist.getPlaylistItems().remove(oldItem);
          log.debug("item deleted " + oldItem.getYoutubeId());
        } else if (!oldItem.getTitle().equals(newItem.getTitle())) {
          changes.add(new PlaylistItemChange(playlist, oldItem.getYoutubeId(), oldItem.getTitle(), newItem.getTitle()));
          oldItem.setTitle(newItem.getTitle());
          log.debug("item renamed " + oldItem.getYoutubeId());
        }
      }
      for (PlaylistItem newItem : new ArrayList<>(newItems)) {
        PlaylistItem oldItem = mappedOld.get(newItem.getYoutubeId());
        if (oldItem == null) {
          playlist.getPlaylistItems().add(newItem);
          log.debug("item added " + newItem.getYoutubeId());
        }
      }

      playlist.setTitle(info.title);
      playlist.setLastUpdate(Instant.now());
    }
    return changes;
  }

  private Map<Account, List<PlaylistChange>> getChangesByAccount(List<PlaylistItemChange> changes) throws SQLException, ReflectiveOperationException {
    Map<Playlist, PlaylistChange> changesByPlaylist = new HashMap<>();
    for (PlaylistItemChange change : changes) {
      changesByPlaylist.computeIfAbsent(change.playlist, PlaylistChange::new).itemChanges.add(change);
    }
    Map<Account, List<PlaylistChange>> changesPerAccount = new HashMap<>();
    for (PlaylistChange playlistChange : changesByPlaylist.values()) {
      for (Account subscriber : playlistChange.playlist.getAccounts()) {
        changesPerAccount.computeIfAbsent(subscriber, s -> new ArrayList<>()).add(playlistChange);
      }
    }
    return changesPerAccount;
  }

  private Set<PlaylistItem> toSet(List<ItemInfo> items) {
    Set<PlaylistItem> result = new HashSet<>();
    for (ItemInfo item : items) {
      if (item.id != null)
        result.add(new PlaylistItem(null, item.id, item.title));
    }
    return result;
  }

  public static class PlaylistChange {

    public final Playlist playlist;
    public final List<PlaylistItemChange> itemChanges = new ArrayList<>();

    public PlaylistChange(Playlist playlist) {
      this.playlist = playlist;
    }

    @Override
    public String toString() {
      return "PlaylistChange{" +
          "playlist=" + playlist +
          ", itemChanges=" + itemChanges +
          '}';
    }
  }

  public static class PlaylistItemChange {

    public final Playlist playlist;
    public final String playlistItem;
    public final String oldTitle;
    public final String newTitle;

    public PlaylistItemChange(Playlist playlist, String playlistItem, String oldTitle, String newTitle) {
      this.playlist = playlist;
      this.playlistItem = playlistItem;
      this.oldTitle = oldTitle;
      this.newTitle = newTitle;
    }

    @Override
    public String toString() {
      return "PlaylistItemChange{" +
          "playlist=" + playlist +
          ", playlistItem='" + playlistItem + '\'' +
          ", oldTitle='" + oldTitle + '\'' +
          ", newTitle='" + newTitle + '\'' +
          '}';
    }
  }
}
