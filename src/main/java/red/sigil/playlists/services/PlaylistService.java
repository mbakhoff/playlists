package red.sigil.playlists.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
      playlist = playlists.save(new Playlist(null, yid, null, Instant.EPOCH));
    }
    accounts.findByEmail(email).getPlaylists().add(playlist);
    log.info("started tracking " + yid + " for " + email);
  }

  public void stopTracking(String email, Set<String> removedIds) {
    List<Playlist> removedPlaylists = playlists.findByYoutubeIdIn(removedIds);
    accounts.findByEmail(email).getPlaylists().removeAll(removedPlaylists);
    for (Playlist playlist : removedPlaylists) {
      if (playlist.getAccounts().isEmpty())
        playlists.delete(playlist);
    }
    log.info("stopped tracking " + removedIds + " for " + email);
  }

  public List<PlaylistItemChange> findPlaylistChanges(Playlist playlist) throws Exception {
    try {
      log.info("updating playlist " + playlist.getYoutubeId());
      PlaylistFetchService.ItemInfo info = playlistFetchService.read(playlist.getYoutubeId());
      playlist.setTitle(info.title);
      playlist.setLastUpdate(Instant.now());
      return pullPlaylistChanges(playlist);
    } catch (PlaylistFetchService.PlaylistNotFound e) {
      log.info("deleting playlist {} - deleted from remote", playlist.getYoutubeId());
      playlists.delete(playlist);
      return Collections.emptyList();
    }
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
        changes.add(new PlaylistItemChange(id, null, pair.newItem.getTitle()));
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
