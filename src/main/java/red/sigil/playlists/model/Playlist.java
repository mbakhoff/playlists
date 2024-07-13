package red.sigil.playlists.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
public class Playlist {

  @Id
  @GeneratedValue
  private Long id;

  @Version
  private Long version;

  private String youtubeId;

  private String title;

  private Instant lastUpdate;

  @OneToOne
  public PlaylistChange lastChange;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public String getYoutubeId() {
    return youtubeId;
  }

  public void setYoutubeId(String youtubeId) {
    this.youtubeId = youtubeId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Instant getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(Instant lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  public PlaylistChange getLastChange() {
    return lastChange;
  }

  public void setLastChange(PlaylistChange lastChange) {
    this.lastChange = lastChange;
  }
}
