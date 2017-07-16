package red.sigil.playlists.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Pageable;
import red.sigil.playlists.entities.Account;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.entities.PlaylistItem;
import red.sigil.playlists.services.AccountRepository;
import red.sigil.playlists.services.PlaylistFetchService;
import red.sigil.playlists.services.PlaylistFetchService.ItemInfo;
import red.sigil.playlists.services.PlaylistRepository;
import red.sigil.playlists.services.PlaylistService;
import red.sigil.playlists.services.PlaylistService.PlaylistItemChange;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(MockitoJUnitRunner.class)
public class PlaylistServiceTest {

  @Mock
  private PlaylistRepository playlistRepository;
  @Mock
  private AccountRepository accountRepository;
  @Mock
  private PlaylistFetchService playlistFetchService;

  private PlaylistService playlistService;

  @Before
  public void setUp() throws Exception {
    playlistService = new PlaylistService(playlistRepository, accountRepository, playlistFetchService);
  }

  @Test
  public void itemRemovalDetected() throws Exception {
    MockPlaylist playlist = new MockPlaylist();
    when(playlistRepository.findAllByOrderByLastUpdateAsc(any(Pageable.class))).thenReturn(asList(playlist));
    when(playlistFetchService.read(anyString())).thenReturn(playlist.info);
    when(playlistFetchService.readItems(anyString())).thenReturn(asList());

    List<PlaylistItemChange> itemChanges = playlistService.findPlaylistChanges(playlist);

    assertEquals("video0", itemChanges.get(0).playlistItem);
    assertEquals("video0title", itemChanges.get(0).oldTitle);
    assertEquals(null, itemChanges.get(0).newTitle);
  }

  @Test
  public void itemRenameDetected() throws Exception {
    MockPlaylist playlist = new MockPlaylist();
    ItemInfo itemInfo = new ItemInfo("video0", "video0titleUpdated");
    when(playlistRepository.findAllByOrderByLastUpdateAsc(any(Pageable.class))).thenReturn(asList(playlist));
    when(playlistFetchService.read(anyString())).thenReturn(playlist.info);
    when(playlistFetchService.readItems(anyString())).thenReturn(asList(itemInfo));

    List<PlaylistItemChange> itemChanges = playlistService.findPlaylistChanges(playlist);

    assertEquals("video0", itemChanges.get(0).playlistItem);
    assertEquals("video0title", itemChanges.get(0).oldTitle);
    assertEquals("video0titleUpdated", itemChanges.get(0).newTitle);
  }

  @Test
  public void playlistTimestampUpdated() throws Exception {
    MockPlaylist playlist = new MockPlaylist();
    when(playlistRepository.findAllByOrderByLastUpdateAsc(any(Pageable.class))).thenReturn(asList(playlist));
    when(playlistFetchService.read(anyString())).thenReturn(playlist.info);
    when(playlistFetchService.readItems(anyString())).thenReturn(asList());

    playlistService.findPlaylistChanges(playlist);
    
    assertTrue("timestamp updated", playlist.getLastUpdate().isAfter(Instant.EPOCH));
  }

  @Test
  public void playlistDeletedWhenNotFound() throws Exception {
    Playlist playlist = new Playlist(0L, "yid0", "pl0", Instant.EPOCH);
    when(playlistRepository.findAllByOrderByLastUpdateAsc(any(Pageable.class))).thenReturn(asList(playlist));
    when(playlistFetchService.read(anyString())).thenThrow(new PlaylistFetchService.PlaylistNotFound("yid0"));

    playlistService.findPlaylistChanges(playlist);

    verify(playlistRepository, Mockito.atLeastOnce()).delete(playlist);
  }

  @Test(expected = IOException.class)
  public void fetchExceptionPropagated() throws Exception {
    MockPlaylist playlist = new MockPlaylist();
    when(playlistRepository.findAllByOrderByLastUpdateAsc(any(Pageable.class))).thenReturn(asList(playlist));
    when(playlistFetchService.read(anyString())).thenReturn(playlist.info);
    when(playlistFetchService.readItems(anyString())).thenThrow(new IOException("MOCK"));

    playlistService.findPlaylistChanges(playlist);
  }

  static class MockPlaylist extends Playlist {
    final ItemInfo info = new ItemInfo("yid0", "pl0");
    public MockPlaylist() {
      super(0L, "yid0", "pl0", Instant.EPOCH);
      getAccounts().add(new Account(0L, "testEmail", "testPassword"));
      getPlaylistItems().add(new PlaylistItem(1L, "video0", "video0title"));
    }
  }
}
