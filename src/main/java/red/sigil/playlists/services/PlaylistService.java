package red.sigil.playlists.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import red.sigil.playlists.model.Account;
import red.sigil.playlists.model.Playlist;
import red.sigil.playlists.model.PlaylistItem;
import red.sigil.playlists.model.PlaylistItemChange;
import red.sigil.playlists.services.PlaylistFetchService.ItemInfo;
import red.sigil.playlists.services.PlaylistFetchService.PlaylistNotFound;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static red.sigil.playlists.utils.CollectionHelper.toMap;

@Component
public class PlaylistService {

  private static Logger log = LoggerFactory.getLogger(PlaylistService.class);

  private final PlaylistRepository playlists;
  private final AccountRepository accounts;
  private final PlaylistFetchService playlistFetchService;

  public PlaylistService(PlaylistRepository playlists, AccountRepository accounts, PlaylistFetchService playlistFetchService) {
    this.playlists = playlists;
    this.accounts = accounts;
    this.playlistFetchService = playlistFetchService;
  }

  public void startTracking(String email, String yid) {
    Playlist playlist = playlists.findByYoutubeId(yid);
    if (playlist == null) {
      playlist = new Playlist(null, yid, null, null);
      playlists.create(playlist);
    }
    Account account = accounts.findByEmail(email);
    playlists.addToAccount(account.getId(), playlist.getId());
    log.info("started tracking " + yid + " for " + email);
  }

  public void stopTracking(String email, String yid) {
    Playlist playlist = playlists.findByYoutubeId(yid);
    Account account = accounts.findByEmail(email);
    playlists.removeFromAccount(account.getId(), playlist.getId());
    log.info("stopped tracking " + yid + " for " + email);
  }

  public void removeOrphans() {
    int count = playlists.removePlaylistOrphans();
    log.info("removed orphaned playlists: " + count);
  }

  public Map<Account, Map<Playlist, List<PlaylistItemChange>>> update() throws Exception {
    return getChangesByAccount(checkRecentPlaylistChanges());
  }

  private Map<Playlist, List<PlaylistItemChange>> checkRecentPlaylistChanges() throws Exception {
    List<Playlist> stale = playlists.findAllByOrderByLastUpdateAsc(10);
    stale.removeIf(p -> p.getLastUpdate() != null && p.getLastUpdate().plus(1, ChronoUnit.HOURS).isAfter(Instant.now()));

    for (Playlist playlist : new ArrayList<>(stale)) {
      log.info("updating playlist " + playlist.getYoutubeId());
      try {
        ItemInfo info = playlistFetchService.read(playlist.getYoutubeId());
        playlist.setTitle(info.title);
      } catch (PlaylistNotFound e) {
        log.info("playlist unavailable: " + playlist.getYoutubeId());
        playlist.setTitle(null);
        stale.remove(playlist);
      }
      playlist.setLastUpdate(Instant.now());
      playlists.update(playlist);
    }

    Map<Playlist, List<PlaylistItemChange>> allChanges = new HashMap<>();
    for (Playlist playlist : stale) {
      List<PlaylistItemChange> changes = doItemChanges(playlist);
      if (!changes.isEmpty())
        allChanges.put(playlist, changes);
    }
    return allChanges;
  }

  private List<PlaylistItemChange> doItemChanges(Playlist playlist) throws Exception {
    Map<String, PlaylistItem> existing = toMap(playlists.findItemsByPlaylist(playlist.getId()), PlaylistItem::getYoutubeId);
    Map<String, ItemInfo> latest = toMap(playlistFetchService.readItems(playlist.getYoutubeId()), i -> i.id);

    List<PlaylistItemChange> changes = new ArrayList<>();
    for (PlaylistItem item : existing.values()) {
      ItemInfo info = latest.get(item.getYoutubeId());
      if (info == null) {
        playlists.deleteItem(item);
        changes.add(new PlaylistItemChange(item.getYoutubeId(), item.getTitle(), null));
      } else if (!Objects.equals(item.getTitle(), info.title)) {
        item.setTitle(info.title);
        playlists.update(item);
        changes.add(new PlaylistItemChange(item.getYoutubeId(), item.getTitle(), info.title));
      }
    }
    for (ItemInfo info : latest.values()) {
      PlaylistItem item = existing.get(info.id);
      if (item == null) {
        item = new PlaylistItem(playlist.getId(), info.id, info.title);
        playlists.insert(item);
        changes.add(new PlaylistItemChange(item.getYoutubeId(), null, info.title));
      }
    }
    return changes;
  }

  private Map<Account, Map<Playlist, List<PlaylistItemChange>>> getChangesByAccount(Map<Playlist, List<PlaylistItemChange>> changes) {
    Map<Account, Map<Playlist, List<PlaylistItemChange>>> result = new HashMap<>();
    changes.forEach((playlist, itemChanges) -> {
      for (Account account : accounts.findByPlaylist(playlist.getId())) {
        result.computeIfAbsent(account, a -> new HashMap<>()).put(playlist, itemChanges);
      }
    });
    return result;
  }

}
