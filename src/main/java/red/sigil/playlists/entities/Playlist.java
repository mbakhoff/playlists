package red.sigil.playlists.entities;

import java.time.Instant;

public class Playlist {

  private Long id;
  private String youtubeId;
  private String title;
  private Instant lastUpdate;

  public Playlist(Long id, String youtubeId, String title, Instant lastUpdate) {
    this.id = id;
    this.youtubeId = youtubeId;
    this.title = title;
    this.lastUpdate = lastUpdate;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Playlist playlist = (Playlist) o;
    return youtubeId != null ? youtubeId.equals(playlist.youtubeId) : playlist.youtubeId == null;
  }

  @Override
  public int hashCode() {
    return youtubeId != null ? youtubeId.hashCode() : 0;
  }
}
