package red.sigil.playlists.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import red.sigil.playlists.ScheduledUpdater.PlaylistChange;
import red.sigil.playlists.ScheduledUpdater.PlaylistItemChange;
import red.sigil.playlists.entities.Playlist;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = NONE, classes = {
    FreemarkerEmailFormatter.class,
    FreeMarkerAutoConfiguration.class
})
public class FreemarkerEmailFormatterTest {

  @Autowired
  private FreemarkerEmailFormatter fmt;

  private List<PlaylistChange> changes;

  @Before
  public void setupPlaylists() {
    PlaylistChange change1 = new PlaylistChange(
        new Playlist(1L, "yid1", "title1", null),
        asList(
            new PlaylistItemChange("item1", "old1", "new1"),
            new PlaylistItemChange("item2", "old2", null)
        )
    );

    PlaylistChange change2 = new PlaylistChange(
        new Playlist(2L, "yid2", "title2", null),
        asList(
            new PlaylistItemChange("item3", "old3", "new3"),
            new PlaylistItemChange("item4", "old4", null)
        )
    );

    changes = asList(change1, change2);
  }

  @Test
  public void testFormatting() throws Exception {
    String message = fmt.generatePlaylistsChangedNotification(changes);
    assertThat(message, containsString("yid1"));
    assertThat(message, containsString("yid2"));
    assertThat(message, containsString("item1"));
    assertThat(message, containsString("item2"));
    assertThat(message, containsString("old1"));
    assertThat(message, containsString("new1"));
    assertThat(message, containsString("old2"));
  }
}
