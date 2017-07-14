package red.sigil.playlists.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import red.sigil.playlists.entities.Account;
import red.sigil.playlists.entities.Playlist;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PlaylistService {

  private static Logger log = LoggerFactory.getLogger(PlaylistService.class);

  private final PlaylistRepository playlists;
  private final AccountRepository accounts;

  @Autowired
  public PlaylistService(PlaylistRepository playlists, AccountRepository accounts) {
    this.playlists = playlists;
    this.accounts = accounts;
  }

  public void startTracking(String email, String url) {
    String yid = parseListId(url);
    Account account = accounts.findByEmail(email);
    Playlist playlist = playlists.findByYoutubeId(yid);
    if (playlist == null) {
      playlist = playlists.save(new Playlist(null, yid, null, Instant.EPOCH));
    }
    account.getPlaylists().add(playlist);
    log.info("started tracking " + yid + " for " + email);
  }

  public void stopTracking(String email, Set<String> toRemove) {
    Account account = accounts.findByEmail(email);
    Set<Playlist> playlists = account.getPlaylists();
    for (Playlist playlist : new ArrayList<>(playlists)) {
      if (toRemove.contains(playlist.getYoutubeId())) {
        playlists.remove(playlist);
        playlist.getAccounts().remove(account);
        if (playlist.getAccounts().isEmpty()) {
          this.playlists.delete(playlist);
        }
      }
    }
    log.info("stopped tracking " + toRemove + " for " + email);
  }

  private String parseListId(String url) {
    // e.g. https://www.youtube.com/playlist?list=PL7USMo--IcSi5p44jTZTezqS3Z-MEC6DU
    Matcher matcher = Pattern.compile(".*[?&]list=([A-Za-z0-9\\-_]+).*").matcher(url);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    throw new IllegalArgumentException(url);
  }
}
