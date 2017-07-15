package red.sigil.playlists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Pageable;
import red.sigil.playlists.ScheduledUpdater.PlaylistChange;
import red.sigil.playlists.entities.Account;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.entities.PlaylistItem;
import red.sigil.playlists.services.EmailService;
import red.sigil.playlists.services.FreemarkerEmailFormatter;
import red.sigil.playlists.services.PlaylistFetchService;
import red.sigil.playlists.services.PlaylistFetchService.ItemInfo;
import red.sigil.playlists.services.PlaylistRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class ScheduledUpdaterTest {

  @Mock
  private PlaylistRepository playlistRepository;
  @Mock
  private PlaylistFetchService playlistFetchService;
  @Mock
  private FreemarkerEmailFormatter emailFormatter;
  @Mock
  private EmailService emailService;

  private ScheduledUpdater scheduledUpdater;

  @Before
  public void setUp() throws Exception {
    scheduledUpdater = new ScheduledUpdater(playlistRepository, playlistFetchService, emailService, emailFormatter);
  }

  @Test
  public void notifiedWhenItemRemoved() throws Exception {
    MockPlaylist playlist = new MockPlaylist();
    when(playlistRepository.findAllByOrderByLastUpdateAsc(any(Pageable.class))).thenReturn(listOf(playlist));
    when(playlistFetchService.read(anyString())).thenReturn(playlist.info);
    when(playlistFetchService.readItems(anyString())).thenReturn(listOf());

    scheduledUpdater.runSyncTasks();

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(emailFormatter, Mockito.atLeastOnce()).generatePlaylistsChangedNotification(captor.capture());
    List<PlaylistChange> value = captor.getValue();
    assertEquals("video0", value.get(0).itemChanges.get(0).playlistItem);
    assertEquals("video0title", value.get(0).itemChanges.get(0).oldTitle);
    assertEquals(null, value.get(0).itemChanges.get(0).newTitle);
  }

  @Test
  public void notifiedWhenItemRenamed() throws Exception {
    MockPlaylist playlist = new MockPlaylist();
    ItemInfo itemInfo = new ItemInfo("video0", "video0titleUpdated");
    when(playlistRepository.findAllByOrderByLastUpdateAsc(any(Pageable.class))).thenReturn(listOf(playlist));
    when(playlistFetchService.read(anyString())).thenReturn(playlist.info);
    when(playlistFetchService.readItems(anyString())).thenReturn(listOf(itemInfo));

    scheduledUpdater.runSyncTasks();

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(emailFormatter, Mockito.atLeastOnce()).generatePlaylistsChangedNotification(captor.capture());
    List<PlaylistChange> value = captor.getValue();
    assertEquals("video0", value.get(0).itemChanges.get(0).playlistItem);
    assertEquals("video0title", value.get(0).itemChanges.get(0).oldTitle);
    assertEquals("video0titleUpdated", value.get(0).itemChanges.get(0).newTitle);
  }

  @Test
  public void playlistTimestampUpdated() throws Exception {
    MockPlaylist playlist = new MockPlaylist();
    when(playlistRepository.findAllByOrderByLastUpdateAsc(any(Pageable.class))).thenReturn(listOf(playlist));
    when(playlistFetchService.read(anyString())).thenReturn(playlist.info);
    when(playlistFetchService.readItems(anyString())).thenReturn(listOf());

    scheduledUpdater.runSyncTasks();
    
    assertTrue("timestamp updated", playlist.getLastUpdate().isAfter(Instant.EPOCH));
  }

  @Test
  public void playlistDeletedWhenNotFound() throws Exception {
    Playlist playlist = new Playlist(0L, "yid0", "pl0", Instant.EPOCH);
    when(playlistRepository.findAllByOrderByLastUpdateAsc(any(Pageable.class))).thenReturn(listOf(playlist));
    when(playlistFetchService.read(anyString())).thenThrow(new PlaylistFetchService.PlaylistNotFound("yid0"));

    scheduledUpdater.runSyncTasks();

    verify(playlistRepository, Mockito.atLeastOnce()).delete(playlist);
  }

  @Test
  public void playlistSkippedOnItemReadException() throws Exception {
    MockPlaylist playlist = new MockPlaylist();
    when(playlistRepository.findAllByOrderByLastUpdateAsc(any(Pageable.class))).thenReturn(listOf(playlist));
    when(playlistFetchService.read(anyString())).thenReturn(playlist.info);
    when(playlistFetchService.readItems(anyString())).thenThrow(new IOException("MOCK"));

    scheduledUpdater.runSyncTasks();

    verify(playlistRepository, Mockito.never()).delete(playlist);
  }

  static class MockPlaylist extends Playlist {
    final ItemInfo info = new ItemInfo("yid0", "pl0");
    public MockPlaylist() {
      super(0L, "yid0", "pl0", Instant.EPOCH);
      getAccounts().add(new Account(0L, "testEmail", "testPassword"));
      getPlaylistItems().add(new PlaylistItem(1L, "video0", "video0title"));
    }
  }

  private <T> Set<T> setOf(T... args) {
    return new HashSet<>(Arrays.asList(args));
  }

  private <T> List<T> listOf(T... args) {
    return new ArrayList<>(Arrays.asList(args));
  }
}
