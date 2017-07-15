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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
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
  public void notifiedWhenPlaylistRemoved() throws Exception {
    Playlist pl = new Playlist(0L, "yid0", "pl0", Instant.EPOCH);
    PlaylistItem item = new PlaylistItem(1L, "video0", "video0title");
    ItemInfo ii = new ItemInfo(pl.getYoutubeId(), pl.getTitle());
    Account account = new Account(0L, "testEmail", "testPassword");

    when(playlistRepository.findAllByOrderByLastUpdateDesc(any(Pageable.class))).thenReturn(listOf(pl));
    pl.setPlaylistItems(setOf(item));
    pl.setAccounts(setOf(account));
    when(playlistFetchService.read(anyString())).thenReturn(ii);
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
  public void notifiedWhenPlaylistRenamed() throws Exception {
    Playlist pl = new Playlist(0L, "yid0", "pl0", Instant.EPOCH);
    PlaylistItem item = new PlaylistItem(1L, "video0", "video0title");
    ItemInfo iiPl = new ItemInfo(pl.getYoutubeId(), pl.getTitle());
    ItemInfo iiItem = new ItemInfo(item.getYoutubeId(), item.getTitle() + "Updated");
    Account account = new Account(0L, "testEmail", "testPassword");

    when(playlistRepository.findAllByOrderByLastUpdateDesc(any(Pageable.class))).thenReturn(listOf(pl));
    pl.setPlaylistItems(setOf(item));
    pl.setAccounts(setOf(account));
    when(playlistFetchService.read(anyString())).thenReturn(iiPl);
    when(playlistFetchService.readItems(anyString())).thenReturn(listOf(iiItem));

    scheduledUpdater.runSyncTasks();

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(emailFormatter, Mockito.atLeastOnce()).generatePlaylistsChangedNotification(captor.capture());
    List<PlaylistChange> value = captor.getValue();
    assertEquals("video0", value.get(0).itemChanges.get(0).playlistItem);
    assertEquals("video0title", value.get(0).itemChanges.get(0).oldTitle);
    assertEquals("video0titleUpdated", value.get(0).itemChanges.get(0).newTitle);
  }

  private <T> Set<T> setOf(T... args) {
    return new HashSet<>(Arrays.asList(args));
  }

  private <T> List<T> listOf(T... args) {
    return new ArrayList<>(Arrays.asList(args));
  }
}
