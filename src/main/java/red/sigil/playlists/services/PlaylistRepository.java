package red.sigil.playlists.services;

import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.entities.PlaylistItem;

import java.util.List;
import java.util.Set;

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

  @SqlQuery("SELECT * FROM playlist WHERE youtube_id IN (<ids>)")
  List<Playlist> findByYoutubeIdIn(@BindList Set<String> ids);

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

  @SqlUpdate("DELETE FROM playlist WHERE id = :id")
  void deletePlaylist(long id);

  @SqlQuery("SELECT * FROM playlist_item pi JOIN playlist_playlist_items ppi ON pi.id=ppi.playlist_items_id WHERE ppi.playlist_id = :id")
  List<PlaylistItem> findItemsByPlaylist(long id);

  @SqlQuery("SELECT * FROM playlist_item WHERE id = :id")
  PlaylistItem findItemById(long id);

  @SqlQuery("SELECT * FROM playlist p JOIN playlist_playlist_items ppi ON p.id=ppi.playlist_id WHERE ppi.playlist_items_id = :id")
  List<Playlist> findPlaylistsByItem(long id);

  @SqlUpdate("INSERT INTO account_playlists (accounts_id, playlists_id) VALUES (:account, :playlist)")
  void addToAccount(long account, long playlist);

  @SqlUpdate("DELETE FROM account_playlists WHERE accounts_id = :account AND playlists_id = :playlist")
  void removeFromAccount(long account, long playlist);

  @SqlUpdate("INSERT INTO playlist_playlist_items (playlist_id, playlist_items_id) VALUES (:playlist, :item)")
  void addToPlaylist(long playlist, long item);

  @SqlUpdate("DELETE FROM playlist_playlist_items WHERE playlist_id = :playlist AND playlist_items_id = :item")
  void removeFromPlaylist(long playlist, long item);

  default void create(PlaylistItem item) {
    long id = getHandle()
        .createUpdate("INSERT INTO playlist_item (id, youtube_id, title) VALUES (nextval('hibernate_sequence'), :youtubeId, :title)")
        .bindBean(item)
        .executeAndReturnGeneratedKeys("id")
        .mapTo(long.class)
        .findOnly();
    item.setId(id);
  }

  @SqlUpdate("UPDATE playlist_item SET youtube_id=:youtubeId, title=:title WHERE id=:id")
  void update(@BindBean PlaylistItem item);

  @SqlUpdate("DELETE FROM playlist_item WHERE id = :id")
  void deleteItem(long id);

  @SqlQuery("SELECT * FROM playlist_item WHERE youtube_id = ?")
  PlaylistItem findItemByYoutubeId(String youtubeId);
}
