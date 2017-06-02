package red.sigil.playlists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import red.sigil.playlists.entities.Account;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.entities.PlaylistItem;
import red.sigil.playlists.services.EmailService;
import red.sigil.playlists.services.PlaylistFetchService;
import red.sigil.playlists.services.PlaylistFetchService.ItemInfo;
import red.sigil.playlists.services.PlaylistService;

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

  @Autowired
  private PlaylistService playlistService;

  @Autowired
  private PlaylistFetchService playlistFetchService;

  @Autowired
  private EmailService emailService;

  @Transactional
  @Scheduled(fixedDelay = 60_000, initialDelay = 3000)
  public void runSyncTasks() throws Exception {
    try {
      doRunSyncTasks();
    } catch (Exception e) {
      log.error("uncaught exception", e);
      throw e;
    }
  }

  private void doRunSyncTasks() throws Exception {
    List<PlaylistItemChange> changes = findChangedPlaylistItems();
    if (!changes.isEmpty()) {
      Map<Account, List<PlaylistChange>> changesByAccount = getChangesByAccount(changes);
      for (Map.Entry<Account, List<PlaylistChange>> entry : changesByAccount.entrySet()) {
        String recipientEmail = entry.getKey().getEmail();
        String message = generateNotificationMessage(entry.getValue());
        try {
          log.info("sending notification to " + recipientEmail + " with " + entry.getValue().size() + " playlists");
          emailService.send(recipientEmail, "Youtube tracks changed", message);
        } catch (Exception e) {
          log.error("failed to send notification to " + recipientEmail, e);
        }
      }
    }
  }

  private List<PlaylistItemChange> findChangedPlaylistItems() throws Exception {
    List<PlaylistItemChange> changes = new ArrayList<>();
    for (Playlist playlist : playlistService.getStalePlaylists()) {
      if (playlist.getLastUpdate().plus(1, ChronoUnit.HOURS).isAfter(Instant.now()))
        continue;

      ItemInfo info = playlistFetchService.read(playlist.getYoutubeId());
      if (info == null) {
        playlistService.deletePlaylist(playlist);
        log.info("deleting playlist " + playlist.getYoutubeId());
        continue;
      }

      log.info("updating playlist " + playlist.getYoutubeId());
      Set<PlaylistItem> oldItems = playlist.getItems();
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
          playlist.getItems().remove(oldItem);
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
          playlist.getItems().add(newItem);
          log.debug("item added " + newItem.getYoutubeId());
        }
      }

      playlist.setTitle(info.title);
      playlist.setLastUpdate(Instant.now());
    }
    return changes;
  }

  private Map<Account, List<PlaylistChange>> getChangesByAccount(List<PlaylistItemChange> changes) {
    Map<Playlist, PlaylistChange> changesByPlaylist = new HashMap<>();
    for (PlaylistItemChange change : changes) {
      changesByPlaylist.computeIfAbsent(change.playlist, PlaylistChange::new).itemChanges.add(change);
    }
    Map<Account, List<PlaylistChange>> changesPerAccount = new HashMap<>();
    for (PlaylistChange playlistChange : changesByPlaylist.values()) {
      for (Account subscriber : playlistService.findSubscribers(playlistChange.playlist)) {
        changesPerAccount.computeIfAbsent(subscriber, s -> new ArrayList<>()).add(playlistChange);
      }
    }
    return changesPerAccount;
  }

  private String generateNotificationMessage(List<PlaylistChange> playlistChanges) {
    StringBuilder sb = new StringBuilder();
    sb.append("<html><body><h1>Playlists have changed</h1>");
    for (PlaylistChange playlistChange : playlistChanges) {
      sb.append("<h2><a href=\"https://www.youtube.com/playlist?list=").append(playlistChange.playlist.getYoutubeId()).append("\">").append(Utils.escapeHtml(playlistChange.playlist.getTitle())).append("</a></h2>");
      sb.append("<ol>");
      for (PlaylistItemChange item : playlistChange.itemChanges) {
        if (item.newTitle == null) {
          sb.append("<li>Deleted <a href=\"https://www.youtube.com/watch?v=").append(item.playlistItem).append("\">").append(Utils.escapeHtml(item.oldTitle)).append("</a></li>");
        } else {
          sb.append("<li>Renamed <a href=\"https://www.youtube.com/watch?v=").append(item.playlistItem).append("\">").append(Utils.escapeHtml(item.oldTitle)).append("</a><br/>New name is ").append(Utils.escapeHtml(item.newTitle)).append("</li>");
        }
      }
      sb.append("</ol>");
    }
    sb.append("<p>Generated by <a href=\"https://playlists.sigil.red\">playlists.sigil.red</a></p></body></html>");
    return sb.toString();
  }

  private Set<PlaylistItem> toSet(List<ItemInfo> items) {
    Set<PlaylistItem> result = new HashSet<>();
    for (ItemInfo item : items) {
      if (item.id != null)
        result.add(new PlaylistItem(item.id, item.title));
    }
    return result;
  }

  static class PlaylistChange {

    final Playlist playlist;
    final List<PlaylistItemChange> itemChanges = new ArrayList<>();

    PlaylistChange(Playlist playlist) {
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

  static class PlaylistItemChange {

    final Playlist playlist;
    final String playlistItem;
    final String oldTitle;
    final String newTitle;

    PlaylistItemChange(Playlist playlist, String playlistItem, String oldTitle, String newTitle) {
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
