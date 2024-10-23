package red.sigil.playlists.services;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import red.sigil.playlists.model.Account;
import red.sigil.playlists.model.Playlist;
import red.sigil.playlists.model.PlaylistChange;
import red.sigil.playlists.model.PlaylistItem;
import red.sigil.playlists.model.PlaylistSubscription;
import red.sigil.playlists.services.PlaylistFetchService.ItemInfo;
import red.sigil.playlists.services.PlaylistFetchService.PlaylistNotFound;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PlaylistService {

  private static final Logger log = LoggerFactory.getLogger(PlaylistService.class);

  @Autowired
  private PlaylistRepository playlists;

  @Autowired
  private PlaylistItemRepository playlistItems;

  @Autowired
  private AccountRepository accounts;

  @Autowired
  private PlaylistSubscriptionRepository subscriptions;

  @Autowired
  private PlaylistChangeRepository changeRepo;

  @Autowired
  private PlaylistFetchService playlistFetchService;

  @Autowired
  private PlaylistNotificationService notificationService;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TransactionTemplate transaction;

  public void startTracking(String email, String yid) {
    Playlist playlist = playlists.findByYoutubeId(yid);
    if (playlist == null) {
      playlist = new Playlist();
      playlist.setYoutubeId(yid);
      entityManager.persist(playlist);
      log.info("started tracking " + yid);
    }

    Account account = accounts.findByName(email);
    var subscription = subscriptions.findByAccountAndPlaylist(account, playlist);
    if (subscription == null) {
      entityManager.persist(new PlaylistSubscription(playlist, account));
      log.info("started tracking " + yid + " for " + email);
    }
  }

  public void stopTracking(String email, String yid) {
    Playlist playlist = playlists.findByYoutubeId(yid);
    Account account = accounts.findByName(email);
    var subscription = subscriptions.findByAccountAndPlaylist(account, playlist);
    if (subscription != null) {
      entityManager.remove(subscription);
      log.info("stopped tracking " + yid + " for " + email);
    }

    var remaining = subscriptions.findByPlaylist(playlist);
    if (remaining.isEmpty()) {
      entityManager.remove(playlist);
      log.info("stopped tracking " + yid);
    }
  }

  public void synchronize() throws Exception {
    syncPlaylists();
    var subscriptions = loadSubscriptionsWithChanges();
    var byAccount = subscriptions.stream().collect(Collectors.groupingBy(PlaylistSubscription::getAccount));
    for (var entry : byAccount.entrySet())
      sendUpdatesBatch(entry.getKey(), entry.getValue());
  }

  private void syncPlaylists() {
    for (Playlist playlist : playlists.findAllTop100ByOrderByLastUpdateAsc()) {
      if (playlist.getLastUpdate() != null && playlist.getLastUpdate().plus(1, ChronoUnit.HOURS).isAfter(Instant.now()))
        continue;

      transaction.execute(txStatus -> {
        try {
          processChanges(playlist);
          return null;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  private void processChanges(Playlist playlist) throws Exception {
    playlist = entityManager.merge(playlist);
    log.info("updating playlist " + playlist.getYoutubeId());

    try {
      var playlistMeta = playlistFetchService.read(playlist.getYoutubeId());
      playlist.setTitle(playlistMeta.title);
    } catch (PlaylistNotFound e) {
      log.info("playlist unavailable: " + playlist.getYoutubeId());
    }

    var oldItems = playlistItems.findByPlaylist(playlist);
    var newItems = playlistFetchService.readItems(playlist.getYoutubeId());
    Map<String, PlaylistItem> allExisting = toMap(oldItems, PlaylistItem::getYoutubeId);
    Map<String, ItemInfo> allLatest = toMap(newItems, i -> i.id);

    PlaylistChange lastChange = null;
    int changes = 0;

    for (PlaylistItem item : allExisting.values()) {
      ItemInfo latest = allLatest.get(item.getYoutubeId());
      if (latest == null) {
        entityManager.persist(lastChange = new PlaylistChange(playlist, item.getYoutubeId(), item.getTitle(), null));
        entityManager.remove(item);
        changes++;
      } else if (!Objects.equals(item.getTitle(), latest.title)) {
        entityManager.persist(lastChange = new PlaylistChange(playlist, item.getYoutubeId(), item.getTitle(), latest.title));
        item.setTitle(latest.title);
        changes++;
      }
    }

    for (ItemInfo info : allLatest.values()) {
      PlaylistItem existing = allExisting.get(info.id);
      if (existing == null) {
        entityManager.persist(lastChange = new PlaylistChange(playlist, info.id, null, info.title));
        existing = new PlaylistItem();
        existing.setPlaylist(playlist);
        existing.setYoutubeId(info.id);
        existing.setTitle(info.title);
        entityManager.persist(existing);
        changes++;
      }
    }

    playlist.setLastUpdate(Instant.now());
    if (lastChange != null)
      playlist.setLastChange(lastChange);
    
    log.info("finished playlist " + playlist.getYoutubeId() + " with " + newItems.size() + " items and " + changes + " changes");
  }

  private List<PlaylistSubscription> loadSubscriptionsWithChanges() {
    return entityManager
        .createQuery("select s from PlaylistSubscription s join s.playlist p where s.lastChange < p.lastChange.id order by s.account.id", PlaylistSubscription.class)
        .setMaxResults(100)
        .getResultList();
  }

  private void sendUpdatesBatch(Account account, List<PlaylistSubscription> subscriptions) {
    var allChanges = new HashMap<Playlist, List<PlaylistChange>>();
    transaction.execute(txStatus -> {
      try {
        for (var subscription : subscriptions) {
          subscription = entityManager.merge(subscription);
          var changes = changeRepo.findByPlaylistAndIdGreaterThanOrderByIdAsc(subscription.getPlaylist(), subscription.getLastChange());
          allChanges.put(subscription.getPlaylist(), changes);
          var newBookmark = changes.get(changes.size() - 1).getId();
          log.info("subscription " + subscription.getId() + " updating " + subscription.getLastChange() + " -> " + newBookmark);
          subscription.setLastChange(newBookmark);
        }
        return null;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    try {
      log.info("sending notification to " + account.getEmail() + " with " + allChanges.size() + " playlists");
      notificationService.sendChangeNotification(account, allChanges);
    } catch (Exception e) {
      log.error("failed to send notification to " + account.getEmail(), e);
    }
  }

  private static <K, V> Map<K, V> toMap(List<V> items, Function<V, K> selector) {
    Map<K, V> result = new HashMap<>();
    for (V item : items) {
      result.put(selector.apply(item), item);
    }
    return result;
  }
}
