package red.sigil.playlists;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import red.sigil.playlists.services.PlaylistService;

@Component
public class ScheduledUpdater {

  @Autowired
  private PlaylistService playlistService;

  @Scheduled(fixedDelay = 60_000, initialDelay = 3000)
  public void synchronize() throws Exception {
    playlistService.synchronize();
  }
}
