package red.sigil.playlists.services;

import org.h2.tools.RunScript;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import red.sigil.playlists.entities.Account;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.entities.PlaylistItem;
import red.sigil.playlists.jdbi.InstantArgumentFactory;
import red.sigil.playlists.jdbi.InstantColumnMapper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RepositoryTest {

  private Connection conn;
  private Handle jdbi;

  @Before
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
    try (Reader reader = Files.newBufferedReader(Paths.get("schema.sql"))) {
      RunScript.execute(conn, reader);
    }
    loadFixture("/fixture.sql");
  }

  private void loadFixture(String name) throws IOException, SQLException {
    try (Reader reader = new InputStreamReader(getClass().getResourceAsStream(name), UTF_8)) {
      RunScript.execute(conn, reader);
    }
  }

  @After
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
      List<Playlist> lists = playlists.findByYoutubeIdIn(new HashSet<>(asList("yid3", "yid5")));
      assertEquals(2, lists.size());
      verifyPlaylist(lists, 3);
      verifyPlaylist(lists, 5);
    }
    {
      List<PlaylistItem> items = playlists.findItemsByPlaylist(4L);
      assertEquals(2, items.size());
      verifyItem(items, 7);
      verifyItem(items, 8);
    }
    {
      PlaylistItem yid7 = playlists.findItemByYoutubeId("yid7");
      verifyItem(singletonList(yid7), 7L);
    }
    {
      PlaylistItem item = playlists.findItemById(7L);
      verifyItem(singletonList(item), 7);
    }
    {
      List<Playlist> lists = playlists.findPlaylistsByItem(8L);
      assertEquals(2, lists.size());
      verifyPlaylist(lists, 4);
      verifyPlaylist(lists, 5);
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
    loadFixture("/fixture_deletable.sql");
    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);
    assertNotNull(playlists.findByYoutubeId("yid9"));
    playlists.deletePlaylist(9L);
    assertNull(playlists.findByYoutubeId("yid9"));
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
  public void addToPlaylist() {
    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);
    assertFalse(playlists.findItemsByPlaylist(3L).stream().anyMatch(p -> p.getId() == 8L));
    playlists.addToPlaylist(3L, 8L);
    assertTrue(playlists.findItemsByPlaylist(3L).stream().anyMatch(p -> p.getId() == 8L));
  }

  @Test
  public void removeFromPlaylist() {
    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);
    assertTrue(playlists.findItemsByPlaylist(3L).stream().anyMatch(p -> p.getId() == 7L));
    playlists.removeFromPlaylist(3L, 7L);
    assertFalse(playlists.findItemsByPlaylist(3L).stream().anyMatch(p -> p.getId() == 7L));
  }

  @Test
  public void createItem() {
    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);
    PlaylistItem item = new PlaylistItem(null, "addedYid", "addedTitle");
    playlists.create(item);
    PlaylistItem added = playlists.findItemById(item.getId());
    assertEquals(item.getTitle(), added.getTitle());
    assertEquals(item.getYoutubeId(), added.getYoutubeId());
  }

  @Test
  public void updateItem() {
    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);
    PlaylistItem yid8 = playlists.findItemById(8L);
    yid8.setTitle("title8_updated");
    yid8.setYoutubeId("yid8_updated");
    playlists.update(yid8);
    PlaylistItem updated = playlists.findItemById(8L);
    assertEquals(yid8.getTitle(), updated.getTitle());
    assertEquals(yid8.getYoutubeId(), updated.getYoutubeId());
  }

  @Test
  public void deleteItem() throws Exception {
    loadFixture("/fixture_deletable.sql");
    PlaylistRepository playlists = jdbi.attach(PlaylistRepository.class);
    assertNotNull(playlists.findItemById(10L));
    playlists.deleteItem(10L);
    assertNull(playlists.findItemById(10L));
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
      if (item.getId() == id) {
        count++;
        assertEquals("yid" + id, item.getYoutubeId());
        assertEquals("title" + id, item.getTitle());
      }
    }
    assertEquals(1, count);
  }
}
