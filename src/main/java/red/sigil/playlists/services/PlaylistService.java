package red.sigil.playlists.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import red.sigil.playlists.Utils;
import red.sigil.playlists.entities.Account;
import red.sigil.playlists.entities.Playlist;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class PlaylistService {

  private static Logger log = LoggerFactory.getLogger(PlaylistService.class);

  @PersistenceContext
  private EntityManager em;

  public Set<Playlist> getPlaylists(String email) {
    return getAccount(email).getPlaylists();
  }

  public List<Playlist> getStalePlaylists() {
    return em.createQuery("from Playlist p order by p.lastUpdate desc", Playlist.class)
        .setMaxResults(30)
        .getResultList();
  }

  public void startTracking(String email, String url) {
    String id = Utils.parseListId(url);
    Playlist playlist;
    try {
      playlist = em.createQuery("from Playlist p where p.youtubeId = :yid", Playlist.class)
          .setParameter("yid", id)
          .getSingleResult();
    } catch (NoResultException e) {
      playlist = new Playlist(id, Instant.EPOCH);
      em.persist(playlist);
    }
    getAccount(email).getPlaylists().add(playlist);
    log.info("started tracking " + id + " for " + email);
  }

  public void stopTracking(String email, Set<String> toRemove) {
    Set<Playlist> playlists = getAccount(email).getPlaylists();
    for (Playlist playlist : new ArrayList<>(playlists)) {
      if (toRemove.contains(playlist.getYoutubeId())) {
        playlists.remove(playlist);
        if (findSubscribers(playlist).isEmpty()) {
          em.remove(playlist);
        }
      }
    }
    log.info("stopped tracking " + toRemove + " for " + email);
  }

  public List<Account> findSubscribers(Playlist playlist) {
    return em.createQuery("from Account a where :playlist member of a.playlists", Account.class)
        .setParameter("playlist", playlist)
        .getResultList();
  }

  public void deletePlaylist(Playlist playlist) {
    List<Account> accounts = findSubscribers(playlist);
    for (Account account : accounts) {
      account.getPlaylists().remove(playlist);
    }
    em.remove(playlist);
  }

  private Account getAccount(String email) {
    // TODO move to a more appropriate class
    return em.createQuery("from Account a where a.email = :email", Account.class)
        .setParameter("email", email)
        .getSingleResult();
  }

}
