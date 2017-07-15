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

import java.util.Arrays;
import java.util.List;

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
    Playlist playlist1 = new Playlist(1L, "yid1", "title1", null);
    PlaylistChange change1 = new PlaylistChange(playlist1);
    change1.itemChanges.add(new PlaylistItemChange(playlist1, "item1", "old1", "new1"));
    change1.itemChanges.add(new PlaylistItemChange(playlist1, "item2", "old2", null));

    Playlist playlist2 = new Playlist(2L, "yid2", "title2", null);
    PlaylistChange change2 = new PlaylistChange(playlist2);
    change2.itemChanges.add(new PlaylistItemChange(playlist2, "item3", "old3", "new3"));
    change2.itemChanges.add(new PlaylistItemChange(playlist2, "item4", "old4", null));

    changes = Arrays.asList(change1, change2);
  }

  @Test
  public void testFormatting() throws Exception {
    String message = fmt.generateNotificationMessage(changes);
    assertThat(message, containsString("yid1"));
    assertThat(message, containsString("yid2"));
    assertThat(message, containsString("item1"));
    assertThat(message, containsString("old1"));
    assertThat(message, containsString("new1"));
    assertThat(message, containsString("item2"));
    assertThat(message, containsString("old2"));
  }
}
