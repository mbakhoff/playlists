package red.sigil.playlists.services;

import org.h2.tools.RunScript;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import red.sigil.playlists.jdbi.InstantArgumentFactory;
import red.sigil.playlists.jdbi.InstantColumnMapper;
import red.sigil.playlists.model.Account;
import red.sigil.playlists.model.Playlist;
import red.sigil.playlists.model.PlaylistItem;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static red.sigil.playlists.utils.CollectionHelper.findFirst;

public class RepositoryTest {

  private Connection conn;
  private Handle jdbi;

  @BeforeEach
  public void setUp() throws Exception {
    conn = DriverManager.getConnection("jdbc:h2:mem:;MODE=PostgreSQL");
    Jdbi jdbi = Jdbi.create(() -> conn);
    jdbi.installPlugin(new SqlObjectPlugin());
    jdbi.registerRowMapper(ConstructorMapper.factory(Account.class));
    jdbi.registerRowMapper(ConstructorMapper.factory(Playlist.class));
    jdbi.registerRowMapper(ConstructorMapper.factory(PlaylistItem.class));
    jdbi.registerColumnMapper(Instant.class, new InstantColumnMapper());
    jdbi.registerArgument(new InstantArgumentFactory());
    this.jdbi = jdbi.open();
    loadFixture("/schema.sql");
    loadFixture("/fixture.sql");
  }

  private void loadFixture(String name) throws IOException, SQLException {
    try (Reader reader = new InputStreamReader(getClass().getResourceAsStream(name), UTF_8)) {
      RunScript.execute(conn, reader);
    }
  }

  @AfterEach
  public void tearDown() throws Exception {
    jdbi.close();
    conn.close();
  }

  @Test
  public void fetchTests() {
    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);
    AccountRepository accounts = jdbi.attach(AccountRepository.class);
    {
      List<Playlist> lists = playlists.findAllByOrderByLastUpdateAsc(10);
      assertEquals(3, lists.size());
      verifyPlaylist(lists, 3);
      verifyPlaylist(lists, 4);
      verifyPlaylist(lists, 5);
    }
    {
      List<Playlist> lists = playlists.findAllByAccount(2L);
      assertEquals(2, lists.size());
      verifyPlaylist(lists, 4);
      verifyPlaylist(lists, 5);
    }
    {
      Playlist yid3 = playlists.findByYoutubeId("yid3");
      verifyPlaylist(singletonList(yid3), 3L);
    }
    {
      List<PlaylistItem> items = playlists.findItemsByPlaylist(4L);
      assertEquals(2, items.size());
      verifyItem(items, 7);
      verifyItem(items, 8);
    }
    {
      Account acc = accounts.findByEmail("email1");
      verifyAccount(singletonList(acc), 1L);
    }
    {
      List<Account> accs = accounts.findByPlaylist(5);
      assertEquals(1, accs.size());
      verifyAccount(accs, 2);
    }
  }

  @Test
  public void createAccount() {
    AccountRepository accounts = jdbi.attach(AccountRepository.class);
    accounts.save(new Account(null, "added", "pass"));
    Account added = accounts.findByEmail("added");
    assertEquals("added", added.getEmail());
    assertEquals("pass", added.getPassword());
  }

  @Test
  public void createPlaylist() {
    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);
    Instant y2016 = Instant.parse("2016-01-01T00:00:00Z");
    playlists.create(new Playlist(null, "added", "title", y2016));
    Playlist added = playlists.findByYoutubeId("added");
    assertEquals("added", added.getYoutubeId());
    assertEquals("title", added.getTitle());
    assertEquals(y2016, added.getLastUpdate());
  }

  @Test
  public void updatePlaylist() {
    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);

    Playlist yid3 = playlists.findByYoutubeId("yid3");
    yid3.setTitle("title3_updated");
    yid3.setYoutubeId("yid3_updated");
    yid3.setLastUpdate(Instant.parse("2008-01-01T00:00:00Z"));
    playlists.update(yid3);

    Playlist yid3updated = playlists.findByYoutubeId(yid3.getYoutubeId());
    assertEquals(yid3.getYoutubeId(), yid3updated.getYoutubeId());
    assertEquals(yid3.getTitle(), yid3updated.getTitle());
    assertEquals(yid3.getLastUpdate(), yid3updated.getLastUpdate());
  }

  @Test
  public void deletePlaylist() throws Exception {
    loadFixture("/fixture_extras.sql");

    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);
    assertNotNull(playlists.findByYoutubeId("yid10"));
    playlists.removePlaylistOrphans();
    assertNull(playlists.findByYoutubeId("yid10"));
  }

  @Test
  public void addToAccount() {
    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);
    assertFalse(playlists.findAllByAccount(1L).stream().anyMatch(p -> p.getId() == 5L));
    playlists.addToAccount(1L, 5L);
    assertTrue(playlists.findAllByAccount(1L).stream().anyMatch(p -> p.getId() == 5L));
  }

  @Test
  public void removeFromAccount() {
    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);
    assertTrue(playlists.findAllByAccount(1L).stream().anyMatch(p -> p.getId() == 4L));
    playlists.removeFromAccount(1L, 4L);
    assertFalse(playlists.findAllByAccount(1L).stream().anyMatch(p -> p.getId() == 4L));
  }

  @Test
  public void createItem() {
    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);
    playlists.insert(new PlaylistItem(3L, "yid10", "title10"));
    PlaylistItem item = findFirst(playlists.findItemsByPlaylist(3L), i -> i.getYoutubeId().equals("yid10"));
    assertEquals("title10", item.getTitle());
  }

  @Test
  public void updateItem() {
    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);
    playlists.update(new PlaylistItem(3L, "yid6", "title6updated"));
    PlaylistItem item = findFirst(playlists.findItemsByPlaylist(3L), i -> i.getYoutubeId().equals("yid6"));
    assertEquals("title6updated", item.getTitle());
  }

  @Test
  public void deleteItem() {
    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);
    playlists.deleteItem(new PlaylistItem(3L, "yid6", "title6"));
    assertNull(findFirst(playlists.findItemsByPlaylist(3L), i -> i.getYoutubeId().equals("yid6")));
  }

  private void verifyAccount(List<Account> accs, long id) {
    int count = 0;
    for (Account acc : accs) {
      if (acc.getId() == id) {
        count++;
        assertEquals("email" + id, acc.getEmail());
        assertEquals("pass" + id, acc.getPassword());
      }
    }
    assertEquals(1, count);
  }

  private void verifyPlaylist(List<Playlist> lists, long id) {
    int count = 0;
    for (Playlist list : lists) {
      if (list.getId() == id) {
        count++;
        Instant lastUpdate = id != 5 ? Instant.parse("2017-01-0" + id + "T00:00:00Z") : null;
        assertEquals("yid" + id, list.getYoutubeId());
        assertEquals("title" + id, list.getTitle());
        assertEquals(lastUpdate, list.getLastUpdate());
      }
    }
    assertEquals(1, count);
  }

  private void verifyItem(List<PlaylistItem> items, long id) {
    int count = 0;
    for (PlaylistItem item : items) {
      if (item.getYoutubeId().equals("yid" + id) && item.getTitle().equals("title" + id)) {
        count++;
      }
    }
    assertEquals(1, count);
  }
}
