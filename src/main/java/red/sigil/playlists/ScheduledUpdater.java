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
import red.sigil.playlists.services.PlaylistFetchService.PlaylistNotFound;
import red.sigil.playlists.services.PlaylistRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class ScheduledUpdater {

  private static final Logger log = LoggerFactory.getLogger(ScheduledUpdater.class);

  private final PlaylistRepository playlistRepository;
  private final PlaylistFetchService playlistFetchService;
  private final EmailService emailService;
  private final FreemarkerEmailFormatter formatter;

  @Autowired
  public ScheduledUpdater(PlaylistRepository playlistRepository, PlaylistFetchService playlistFetchService, EmailService emailService, FreemarkerEmailFormatter formatter) {
    this.playlistRepository = playlistRepository;
    this.playlistFetchService = playlistFetchService;
    this.emailService = emailService;
    this.formatter = formatter;
  }

  @Transactional
  @Scheduled(fixedDelay = 60_000, initialDelay = 3000)
  public void runSyncTasks() throws Exception {
    Set<PlaylistChange> changes = pullPlaylistChanges();
    getChangesByAccount(changes).forEach((account, changesForAccount) -> {
      String sendTo = account.getEmail();
      try {
        log.info("sending notification to " + sendTo + " with " + changesForAccount.size() + " playlists");
        String message = formatter.generatePlaylistsChangedNotification(changesForAccount);
        emailService.sendHtml(sendTo, "Youtube tracks changed", message);
      } catch (Exception e) {
        log.error("failed to send notification to " + sendTo, e);
      }
    });
  }

  private Set<PlaylistChange> pullPlaylistChanges() throws Exception {
    Set<PlaylistChange> playlistChanges = new HashSet<>();
    for (Playlist playlist : playlistRepository.findAllByOrderByLastUpdateAsc(new PageRequest(0, 30))) {
      if (playlist.getLastUpdate().plus(1, ChronoUnit.HOURS).isAfter(Instant.now()))
        continue;

      try {
        log.info("updating playlist " + playlist.getYoutubeId());
        ItemInfo info = playlistFetchService.read(playlist.getYoutubeId());
        playlist.setTitle(info.title);
        playlist.setLastUpdate(Instant.now());
        List<PlaylistItemChange> changeList = pullPlaylistChanges(playlist);
        if (!changeList.isEmpty()) {
          playlistChanges.add(new PlaylistChange(playlist, changeList));
        }
      } catch (PlaylistNotFound e) {
        log.info("deleting playlist {} - deleted from remote", playlist.getYoutubeId());
        playlistRepository.delete(playlist);
      } catch (Exception e) {
        log.error("failed to update " + playlist.getYoutubeId(), e);
      }
    }
    return playlistChanges;
  }

  private List<PlaylistItemChange> pullPlaylistChanges(Playlist playlist) throws Exception {
    Map<String, OldAndNew> all = new HashMap<>();
    for (PlaylistItem item : asPlaylistItems(playlistFetchService.readItems(playlist.getYoutubeId())))
      all.computeIfAbsent(item.getYoutubeId(), id -> new OldAndNew()).newItem = item;
    for (PlaylistItem item : playlist.getPlaylistItems())
      all.computeIfAbsent(item.getYoutubeId(), id -> new OldAndNew()).oldItem = item;

    List<PlaylistItemChange> changes = new ArrayList<>();
    all.forEach((id, pair) -> {
      if (pair.newItem == null) {
        changes.add(new PlaylistItemChange(id, pair.oldItem.getTitle(), null));
        playlist.getPlaylistItems().remove(pair.oldItem);
        log.debug("item deleted " + id);
      } else if (pair.oldItem == null) {
        playlist.getPlaylistItems().add(pair.newItem);
        log.debug("item added " + id);
      } else if (!Objects.equals(pair.oldItem.getTitle(), pair.newItem.getTitle())) {
        changes.add(new PlaylistItemChange(id, pair.oldItem.getTitle(), pair.newItem.getTitle()));
        pair.oldItem.setTitle(pair.newItem.getTitle());
        log.debug("item renamed " + id);
      }
    });
    return changes;
  }

  private Map<Account, List<PlaylistChange>> getChangesByAccount(Set<PlaylistChange> changes) {
    Map<Account, List<PlaylistChange>> changesPerAccount = new HashMap<>();
    for (PlaylistChange playlistChange : changes) {
      for (Account subscriber : playlistChange.playlist.getAccounts()) {
        changesPerAccount.computeIfAbsent(subscriber, s -> new ArrayList<>()).add(playlistChange);
      }
    }
    return changesPerAccount;
  }

  private Set<PlaylistItem> asPlaylistItems(List<ItemInfo> items) {
    Set<PlaylistItem> result = new HashSet<>();
    for (ItemInfo item : items) {
      if (item.id != null)
        result.add(new PlaylistItem(null, item.id, item.title));
    }
    return result;
  }

  private static class OldAndNew {
    PlaylistItem oldItem, newItem;
  }

  public static class PlaylistChange {

    public final Playlist playlist;
    public final List<PlaylistItemChange> itemChanges;

    public PlaylistChange(Playlist playlist, List<PlaylistItemChange> itemChanges) {
      this.playlist = playlist;
      this.itemChanges = itemChanges;
    }

    public Playlist getPlaylist() {
      return playlist;
    }

    public List<PlaylistItemChange> getItemChanges() {
      return itemChanges;
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

    public final String playlistItem;
    public final String oldTitle;
    public final String newTitle;

    public PlaylistItemChange(String playlistItem, String oldTitle, String newTitle) {
      this.playlistItem = playlistItem;
      this.oldTitle = oldTitle;
      this.newTitle = newTitle;
    }

    public String getPlaylistItem() {
      return playlistItem;
    }

    public String getOldTitle() {
      return oldTitle;
    }

    public String getNewTitle() {
      return newTitle;
    }

    @Override
    public String toString() {
      return "PlaylistItemChange{" +
          "playlistItem='" + playlistItem + '\'' +
          ", oldTitle='" + oldTitle + '\'' +
          ", newTitle='" + newTitle + '\'' +
          '}';
    }
  }
}
