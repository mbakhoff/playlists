package red.sigil.playlists.entities;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Playlist {

  @Id
  @GeneratedValue
  private Long id;

  @Version
  private Long version;

  @Column(unique = true)
  private String youtubeId;

  private String title;

  private Instant lastUpdate;

  @ElementCollection
  private Set<PlaylistItem> items;

  public Playlist() {
  }

  public Playlist(String youtubeId, Instant lastUpdate) {
    this.youtubeId = youtubeId;
    this.lastUpdate = lastUpdate;
    this.items = new HashSet<>();
  }

  public String getYoutubeId() {
    return youtubeId;
  }

  public Instant getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(Instant lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  public Set<PlaylistItem> getItems() {
    return items;
  }

  public void setItems(Set<PlaylistItem> items) {
    this.items = items;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
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
