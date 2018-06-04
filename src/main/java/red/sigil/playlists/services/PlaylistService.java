package red.sigil.playlists.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import red.sigil.playlists.entities.Account;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.entities.PlaylistItem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PlaylistService {

  private static Logger log = LoggerFactory.getLogger(PlaylistService.class);

  private final PlaylistRepository playlists;
  private final AccountRepository accounts;
  private final PlaylistFetchService playlistFetchService;

  @Autowired
  public PlaylistService(PlaylistRepository playlists, AccountRepository accounts, PlaylistFetchService playlistFetchService) {
    this.playlists = playlists;
    this.accounts = accounts;
    this.playlistFetchService = playlistFetchService;
  }

  public void startTracking(String email, String url) {
    String yid = parseListId(url);
    Playlist playlist = playlists.findByYoutubeId(yid);
    if (playlist == null) {
      playlist = new Playlist(null, yid, null, null);
      playlists.create(playlist);
    }
    Account account = accounts.findByEmail(email);
    playlists.addToAccount(account.getId(), playlist.getId());
    log.info("started tracking " + yid + " for " + email);
  }

  public void stopTracking(String email, Set<String> removedIds) {
    List<Playlist> removedPlaylists = playlists.findByYoutubeIdIn(removedIds);
    Account account = accounts.findByEmail(email);
    for (Playlist removedPlaylist : removedPlaylists) {
      playlists.removeFromAccount(account.getId(), removedPlaylist.getId());
    }
    for (Playlist playlist : removedPlaylists) {
      if (accounts.findByPlaylist(playlist.getId()).isEmpty()) {
        log.info("stopped syncing {} - no users request it", playlist.getYoutubeId());
        deletePlaylist(playlist);
      }
    }
    log.info("stopped tracking " + removedIds + " for " + email);
  }

  public List<PlaylistItemChange> findPlaylistChanges(Playlist playlist) throws Exception {
    try {
      log.info("updating playlist " + playlist.getYoutubeId());
      PlaylistFetchService.ItemInfo info = playlistFetchService.read(playlist.getYoutubeId());
      playlist.setTitle(info.title);
      playlist.setLastUpdate(Instant.now());
      playlists.update(playlist);
      return pullPlaylistChanges(playlist);
    } catch (PlaylistFetchService.PlaylistNotFound e) {
      log.info("deleting playlist {} - deleted from remote", playlist.getYoutubeId());
      deletePlaylist(playlist);
      return Collections.emptyList();
    }
  }

  private void deletePlaylist(Playlist playlist) {
    List<PlaylistItem> items = playlists.findItemsByPlaylist(playlist.getId());
    for (PlaylistItem item : items) {
      playlists.removeFromPlaylist(playlist.getId(), item.getId());
      log.info("removed item {} from {}", item.getYoutubeId(), playlist.getYoutubeId());
    }
    for (PlaylistItem item : items) {
      if (playlists.findPlaylistsByItem(item.getId()).isEmpty()) {
        log.info("dropping track {} - containing playlists deleted", item.getYoutubeId());
        playlists.deleteItem(item.getId());
      }
    }
    playlists.deletePlaylist(playlist.getId());
  }

  private List<PlaylistItemChange> pullPlaylistChanges(Playlist playlist) throws Exception {
    Map<String, OldAndNew> all = new HashMap<>();
    for (PlaylistItem item : asPlaylistItems(playlistFetchService.readItems(playlist.getYoutubeId())))
      all.computeIfAbsent(item.getYoutubeId(), id -> new OldAndNew()).newItem = item;
    for (PlaylistItem item : playlists.findItemsByPlaylist(playlist.getId()))
      all.computeIfAbsent(item.getYoutubeId(), id -> new OldAndNew()).oldItem = item;

    List<PlaylistItemChange> changes = new ArrayList<>();
    all.forEach((id, pair) -> {
      if (pair.newItem == null) {
        changes.add(new PlaylistItemChange(id, pair.oldItem.getTitle(), null));
        playlists.removeFromPlaylist(playlist.getId(), pair.oldItem.getId());
        log.info("removed item {} from {}", pair.oldItem.getYoutubeId(), playlist.getYoutubeId());
        if (playlists.findPlaylistsByItem(pair.oldItem.getId()).isEmpty()) {
          log.info("dropping track {} - last synced playlist removed it", pair.oldItem.getYoutubeId());
          playlists.deleteItem(pair.oldItem.getId());
        }
      } else if (pair.oldItem == null) {
        changes.add(new PlaylistItemChange(id, null, pair.newItem.getTitle()));
        PlaylistItem item = playlists.findItemByYoutubeId(pair.newItem.getYoutubeId());
        if (item == null) {
          playlists.create(pair.newItem);
          item = pair.newItem;
          log.info("initialized item {}", item.getYoutubeId());
        } else if (!Objects.equals(item.getTitle(), pair.newItem.getTitle())) {
          item.setTitle(pair.newItem.getTitle());
          playlists.update(item);
          log.info("updated item {}", item.getYoutubeId());
        }
        playlists.addToPlaylist(playlist.getId(), item.getId());
        log.info("added item {} to {}", item.getYoutubeId(), playlist.getYoutubeId());
      } else if (!Objects.equals(pair.oldItem.getTitle(), pair.newItem.getTitle())) {
        changes.add(new PlaylistItemChange(id, pair.oldItem.getTitle(), pair.newItem.getTitle()));
        pair.oldItem.setTitle(pair.newItem.getTitle());
        playlists.update(pair.oldItem);
        log.info("updated item {}", pair.oldItem.getYoutubeId());
      }
    });
    return changes;
  }

  private Set<PlaylistItem> asPlaylistItems(List<PlaylistFetchService.ItemInfo> items) {
    Set<PlaylistItem> result = new HashSet<>();
    for (PlaylistFetchService.ItemInfo item : items) {
      if (item.id != null)
        result.add(new PlaylistItem(null, item.id, item.title));
    }
    return result;
  }

  private String parseListId(String url) {
    // e.g. https://www.youtube.com/playlist?list=PL7USMo--IcSi5p44jTZTezqS3Z-MEC6DU
    Matcher matcher = Pattern.compile(".*[?&]list=([A-Za-z0-9\\-_]+).*").matcher(url);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    throw new IllegalArgumentException(url);
  }

  private static class OldAndNew {
    PlaylistItem oldItem, newItem;
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
