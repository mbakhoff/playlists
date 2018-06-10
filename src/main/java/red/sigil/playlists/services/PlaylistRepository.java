package red.sigil.playlists.services;

import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import red.sigil.playlists.model.Playlist;
import red.sigil.playlists.model.PlaylistItem;

import java.util.List;

public interface PlaylistRepository extends SqlObject {

  @SqlQuery("SELECT * FROM playlist ORDER BY last_update ASC LIMIT ?")
  List<Playlist> findAllByOrderByLastUpdateAsc(int limit);

  @SqlQuery("" +
      "SELECT * FROM playlist p" +
      "  JOIN account_playlists ap ON p.id = ap.playlists_id" +
      "  WHERE ap.accounts_id = :id")
  List<Playlist> findAllByAccount(long id);

  @SqlQuery("SELECT * FROM playlist WHERE youtube_id=?")
  Playlist findByYoutubeId(String id);

  default void create(Playlist playlist) {
    long id = getHandle()
        .createUpdate("INSERT INTO playlist (id, youtube_id, title, last_update) VALUES (nextval('hibernate_sequence'), :youtubeId, :title, :lastUpdate)")
        .bindBean(playlist)
        .executeAndReturnGeneratedKeys("id")
        .mapTo(long.class)
        .findOnly();
    playlist.setId(id);
  }

  @SqlUpdate("UPDATE playlist SET youtube_id=:youtubeId, title=:title, last_update=:lastUpdate WHERE id = :id")
  void update(@BindBean Playlist playlist);

  @SqlUpdate("INSERT INTO account_playlists (accounts_id, playlists_id) VALUES (:account, :playlist)")
  void addToAccount(long account, long playlist);

  @SqlUpdate("DELETE FROM account_playlists WHERE accounts_id = :account AND playlists_id = :playlist")
  void removeFromAccount(long account, long playlist);

  @SqlUpdate("DELETE FROM playlist WHERE id IN (SELECT id FROM playlist LEFT JOIN account_playlists ON id = playlists_id GROUP BY id HAVING count(accounts_id) = 0)")
  int removePlaylistOrphans();

  @SqlQuery("SELECT * FROM playlist_item WHERE playlist_id = :id")
  List<PlaylistItem> findItemsByPlaylist(long id);

  @SqlUpdate("INSERT INTO playlist_item (playlist_id, youtube_id, title) VALUES (:playlistId, :youtubeId, :title)")
  void insert(@BindBean PlaylistItem item);

  @SqlUpdate("UPDATE playlist_item SET title = :title WHERE playlist_id = :playlistId AND youtube_id = :youtubeId")
  void update(@BindBean PlaylistItem item);

  @SqlUpdate("DELETE FROM playlist_item WHERE playlist_id = :playlistId AND youtube_id = :youtubeId")
  void deleteItem(@BindBean PlaylistItem item);
}
