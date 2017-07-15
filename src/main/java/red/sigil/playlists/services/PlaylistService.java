package red.sigil.playlists.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import red.sigil.playlists.entities.Playlist;

import java.time.Instant;
import java.util.List;
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

  private String parseListId(String url) {
    // e.g. https://www.youtube.com/playlist?list=PL7USMo--IcSi5p44jTZTezqS3Z-MEC6DU
    Matcher matcher = Pattern.compile(".*[?&]list=([A-Za-z0-9\\-_]+).*").matcher(url);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    throw new IllegalArgumentException(url);
  }
}
