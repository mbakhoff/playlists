package red.sigil.playlists.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringRunner;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.services.PlaylistService.PlaylistItemChange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = NONE, classes = {
    ThymeleafAutoConfiguration.class,
    PlaylistNotificationService.class,
    PlaylistNotificationServiceTest.MockSenderConfig.class
})
public class PlaylistNotificationServiceTest {

  @Autowired
  private PlaylistNotificationService service;

  @Test
  public void testFormatting() throws Exception {
    Map<Playlist, List<PlaylistItemChange>> changes = new HashMap<>();
    changes.put(
        new Playlist(1L, "yid1", "title1", null),
        asList(
            new PlaylistItemChange("item1", "old1", "new1"),
            new PlaylistItemChange("item2", "old2", null)
        )
    );
    changes.put(
        new Playlist(2L, "yid2", "title2", null),
        asList(
            new PlaylistItemChange("item3", "old3", "new3"),
            new PlaylistItemChange("item4", "old4", null)
        )
    );

    String message = service.generateNotification(changes);
    assertThat(message, containsString("yid1"));
    assertThat(message, containsString("yid2"));
    assertThat(message, containsString("item1"));
    assertThat(message, containsString("item2"));
    assertThat(message, containsString("old1"));
    assertThat(message, containsString("new1"));
    assertThat(message, containsString("old2"));
  }

  @Configuration
  static class MockSenderConfig {
    @Bean
    JavaMailSender mailSender() {
      return mock(JavaMailSender.class);
    }
  }
}
