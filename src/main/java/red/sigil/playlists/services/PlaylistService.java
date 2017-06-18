package red.sigil.playlists.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import red.sigil.playlists.entities.Account;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.entities.PlaylistItem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PlaylistService {

  private static Logger log = LoggerFactory.getLogger(PlaylistService.class);

  private final JdbcTemplate jdbc;

  @Autowired
  public PlaylistService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Playlist> getPlaylistsByEmail(String email) throws SQLException, ReflectiveOperationException {
    return jdbc.query("" +
            "SELECT * FROM playlist p" +
            " JOIN account_playlist ap ON p.id = ap.playlists_id" +
            " JOIN account a ON a.id = ap.account_id" +
            " WHERE a.email = ?",
        (rs, rowNum) -> mapPlaylist(rs),
        email
    );
  }

  private Playlist mapPlaylist(ResultSet rs) throws SQLException {
    return new Playlist(
        rs.getLong("id"),
        rs.getString("youtubeId"),
        rs.getString("title"),
        rs.getTimestamp("lastUpdate").toInstant()
    );
  }

  public List<Playlist> getPlaylistsByLastUpdate() throws SQLException, ReflectiveOperationException {
    return jdbc.query("" +
            "SELECT * FROM playlist" +
            " ORDER BY lastUpdate DESC" +
            " LIMIT 30",
        (rs, rowNum) -> mapPlaylist(rs)
    );
  }

  public void startTracking(String email, String url) throws SQLException, ReflectiveOperationException {
    String yid = parseListId(url);

    Playlist playlist = new Playlist(null, yid, null, Instant.EPOCH);
    insert(playlist);

    int result = jdbc.update("" +
            "INSERT INTO account_playlist (account_id, playlists_id) " +
            " VALUES ((SELECT id FROM account WHERE email = ?), ?);",
        email,
        playlist.getId()
    );
    if (result != 1)
      throw new SQLException("email " + email + " pl " + playlist.getId());
    log.info("started tracking " + yid + " for " + email);
  }

  private void insert(Playlist playlist) throws SQLException {
    Long id = jdbc.queryForObject("" +
            "INSERT INTO playlist as p (youtubeId, lastUpdate) " +
            " VALUES (?, ?) " +
            " ON CONFLICT (youtubeId) DO UPDATE SET youtubeId = p.youtubeId" +
            " RETURNING id;",
        Long.class,
        playlist.getYoutubeId(),
        Timestamp.from(playlist.getLastUpdate())
    );

    if (id == null)
      throw new SQLException("yid " + playlist.getYoutubeId());
    playlist.setId(id);
  }

  public void stopTracking(String email, Set<String> toRemove) throws SQLException, ReflectiveOperationException {
    List<Playlist> playlists = getPlaylistsByEmail(email);
    for (Playlist playlist : new ArrayList<>(playlists)) {
      if (toRemove.contains(playlist.getYoutubeId())) {
        unlinkPlaylist(email, playlist);
        List<Account> subscribers = findSubscribers(playlist);
        if (subscribers.isEmpty()) {
          deletePlaylist(playlist);
        }
      }
    }
    log.info("stopped tracking " + toRemove + " for " + email);
  }

  private void deletePlaylist(Playlist playlist) throws SQLException {
    int result = jdbc.update("" +
            "DELETE FROM playlist " +
            " WHERE id = ?;",
        playlist.getId()
    );
    if (result != 1)
      throw new SQLException("playlist " + playlist.getId());
  }

  private void unlinkPlaylist(String email, Playlist playlist) throws SQLException {
    int result = jdbc.update("" +
            "DELETE FROM account_playlist" +
            " WHERE playlists_id = ? " +
            "   AND account_id = (SELECT id FROM account WHERE email = ?);",
        playlist.getId(),
        email
    );
    if (result != 1)
      throw new SQLException("pl " + playlist.getId() + " email " + email);
  }

  public List<Account> findSubscribers(Playlist playlist) throws SQLException, ReflectiveOperationException {
    return jdbc.query("" +
            "SELECT a.* FROM account a" +
            " JOIN account_playlist ap ON a.id = ap.account_id" +
            " WHERE ap.playlists_id = ?",
        (rs, rowNum) -> mapAccount(rs),
        playlist.getId()
    );
  }

  private Account mapAccount(ResultSet rs) throws SQLException {
    return new Account(
        rs.getLong("id"),
        rs.getString("email"),
        rs.getString("password")
    );
  }

  public void unlinkAndDeletePlaylist(Playlist playlist) throws SQLException, ReflectiveOperationException {
    List<Account> accounts = findSubscribers(playlist);
    for (Account account : accounts) {
      unlinkPlaylist(account.getEmail(), playlist);
    }
    deletePlaylist(playlist);
  }

  public List<PlaylistItem> getItems(Playlist playlist) throws SQLException, ReflectiveOperationException {
    return jdbc.query("" +
            "SELECT * FROM playlist_items " +
            " WHERE playlist_id = ?;",
        (rs, rowNum) -> mapPlaylistItem(rs),
        playlist.getId()
    );
  }

  private PlaylistItem mapPlaylistItem(ResultSet rs) throws SQLException {
    return new PlaylistItem(
        rs.getLong("id"),
        rs.getString("youtubeId"),
        rs.getString("title")
    );
  }

  public void removeItem(PlaylistItem item) throws SQLException {
    int result = jdbc.update("" +
            "DELETE FROM playlist_items " +
            " WHERE id = ?;",
        item.getId()
    );
    if (result != 1)
      throw new SQLException("item " + item.getId());
  }

  public void update(Playlist playlist) throws SQLException {
    int result = jdbc.update("" +
            "UPDATE playlist " +
            " SET youtubeId=?, title=?, lastUpdate=? " +
            " WHERE id = ?;",
        playlist.getYoutubeId(),
        playlist.getTitle(),
        Timestamp.from(playlist.getLastUpdate()),
        playlist.getId()
    );
    if (result != 1)
      throw new SQLException("playlist " + playlist.getId());
  }

  public void insert(Playlist playlist, PlaylistItem item) throws SQLException {
    Long id = jdbc.queryForObject("" +
            "INSERT INTO playlist_items (playlist_id, youtubeId, title) " +
            " VALUES (?, ?, ?) " +
            " RETURNING id;",
        Long.class,
        playlist.getId(),
        item.getYoutubeId(),
        item.getTitle()
    );
    if (id == null)
      throw new SQLException("pl " + playlist.getId() + " yid " + item.getYoutubeId());
    item.setId(id);
  }

  public void update(PlaylistItem item) throws SQLException {
    int result = jdbc.update("" +
            "UPDATE playlist_items " +
            " SET youtubeId = ?, title = ? " +
            " WHERE id = ?;",
        item.getYoutubeId(),
        item.getTitle(),
        item.getId()
    );
    if (result != 1)
      throw new SQLException("playlist " + item.getId());
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
