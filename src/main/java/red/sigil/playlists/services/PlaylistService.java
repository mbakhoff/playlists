package red.sigil.playlists.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import red.sigil.playlists.Utils;
import red.sigil.playlists.entities.Account;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.entities.PlaylistItem;
import red.sigil.playlists.tx.TransactionAwareConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class PlaylistService {

  private static Logger log = LoggerFactory.getLogger(PlaylistService.class);

  private final TransactionAwareConnection conn;

  @Autowired
  public PlaylistService(TransactionAwareConnection conn) {
    this.conn = conn;
  }

  public List<Playlist> getPlaylistsByEmail(String email) throws SQLException, ReflectiveOperationException {
    ResultSet rs = executeQuery("" +
            "SELECT * FROM playlist p" +
            " JOIN account_playlist ap ON p.id = ap.playlists_id" +
            " JOIN account a ON a.id = ap.account_id" +
            " WHERE a.email = ?",
        email);

    return mapRows(rs, this::mapPlaylist);
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
    ResultSet rs = executeQuery("" +
        "SELECT * FROM playlist" +
        " ORDER BY lastUpdate DESC" +
        " LIMIT 30");
    return mapRows(rs, this::mapPlaylist);
  }

  public void startTracking(String email, String url) throws SQLException, ReflectiveOperationException {
    String yid = Utils.parseListId(url);

    Playlist playlist = new Playlist(null, yid, null, Instant.EPOCH);
    insert(playlist);

    int result = executeUpdate("" +
            "INSERT INTO account_playlist (account_id, playlists_id) " +
            " VALUES ((SELECT id FROM account WHERE email = ?), ?);",
        email,
        playlist.getId());
    if (result != 1)
      throw new SQLException();
    log.info("started tracking " + yid + " for " + email);
  }

  private void insert(Playlist playlist) throws SQLException {
    ResultSet rs = executeQuery("" +
            "INSERT INTO playlist as p (youtubeId, lastUpdate) " +
            " VALUES (?, ?) " +
            " ON CONFLICT (youtubeId) DO UPDATE SET youtubeId = p.youtubeId" +
            " RETURNING id;",
        playlist.getYoutubeId(),
        Timestamp.from(playlist.getLastUpdate()));

    if (!rs.next())
      throw new SQLException();
    playlist.setId(rs.getLong("id"));
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
    int result = executeUpdate("" +
            "DELETE FROM playlist " +
            " WHERE id = ?;",
        playlist.getId());
    if (result != 1)
      throw new SQLException();
  }

  private void unlinkPlaylist(String email, Playlist playlist) throws SQLException {
    int result = executeUpdate("" +
            "DELETE FROM account_playlist" +
            " WHERE playlists_id = ? " +
            "   AND account_id = (SELECT id FROM account WHERE email = ?);",
        playlist.getId(),
        email);
    if (result != 1)
      throw new SQLException();
  }

  public List<Account> findSubscribers(Playlist playlist) throws SQLException, ReflectiveOperationException {
    ResultSet rs = executeQuery("" +
            "SELECT a.* FROM account a" +
            " JOIN account_playlist ap ON a.id = ap.account_id" +
            " WHERE ap.playlists_id = ?",
        playlist.getId());

    return mapRows(rs, this::mapAccount);
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
    ResultSet rs = executeQuery("" +
            "SELECT * FROM playlist_items " +
            " WHERE playlist_id = ?;",
        playlist.getId());
    return mapRows(rs, this::mapPlaylistItem);
  }

  private PlaylistItem mapPlaylistItem(ResultSet rs) throws SQLException {
    return new PlaylistItem(
        rs.getLong("id"),
        rs.getString("youtubeId"),
        rs.getString("title")
    );
  }

  public void removeItem(PlaylistItem item) throws SQLException {
    int result = executeUpdate("" +
            "DELETE FROM playlist_items " +
            " WHERE id = ?;",
        item.getId());
    if (result != 1)
      throw new SQLException();
  }

  public void update(Playlist playlist) throws SQLException {
    int result = executeUpdate("" +
            "UPDATE playlist " +
            " SET youtubeId=?, title=?, lastUpdate=? " +
            " WHERE id = ?;",
        playlist.getYoutubeId(),
        playlist.getTitle(),
        Timestamp.from(playlist.getLastUpdate()),
        playlist.getId());
    if (result != 1)
      throw new SQLException();
  }

  public void insert(Playlist playlist, PlaylistItem item) throws SQLException {
    ResultSet rs = executeQuery("" +
            "INSERT INTO playlist_items (playlist_id, youtubeId, title) " +
            " VALUES (?, ?, ?) " +
            " RETURNING id;",
        playlist.getId(),
        item.getYoutubeId(),
        item.getTitle());
    if (!rs.next())
      throw new SQLException();
    item.setId(rs.getLong("id"));
  }

  public void update(PlaylistItem item) throws SQLException {
    int result = executeUpdate("" +
            "UPDATE playlist " +
            " SET youtubeId = ?, title = ? " +
            " WHERE id = ?;",
        item.getYoutubeId(),
        item.getTitle(),
        item.getId());
    if (result != 1)
      throw new SQLException();
  }

  private static <T> List<T> mapRows(ResultSet rs, Mapper<T> mapper) throws SQLException {
    List<T> result = new ArrayList<>();
    while (rs.next()) {
      result.add(mapper.apply(rs));
    }
    return result;
  }

  private int executeUpdate(String sql, Object... params) throws SQLException {
    return prepare(sql, params).executeUpdate();
  }

  private ResultSet executeQuery(String sql, Object... params) throws SQLException {
    return prepare(sql, params).executeQuery();
  }

  private PreparedStatement prepare(String sql, Object... params) throws SQLException {
    PreparedStatement ps = conn.prepareStatement(sql);
    for (int i = 0; i < params.length; i++) {
      ps.setObject(i + 1, params[i]);
    }
    return ps;
  }

  private interface Mapper<T> {
    T apply(ResultSet rs) throws SQLException;
  }
}
