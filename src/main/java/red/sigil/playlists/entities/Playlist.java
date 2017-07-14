package red.sigil.playlists.entities;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Playlist {

  @Id
  @GeneratedValue
  private Long id;
  private String youtubeId;
  private String title;
  private Instant lastUpdate;

  @ManyToMany(cascade = CascadeType.ALL, mappedBy = "playlists")
  private Set<Account> accounts = new HashSet<>();

  @OneToMany(cascade = CascadeType.ALL)
  private Set<PlaylistItem> playlistItems = new HashSet<>();

  protected Playlist() {
  }

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

  public Set<Account> getAccounts() {
    return accounts;
  }

  public void setAccounts(Set<Account> accounts) {
    this.accounts = accounts;
  }

  public Set<PlaylistItem> getPlaylistItems() {
    return playlistItems;
  }

  public void setPlaylistItems(Set<PlaylistItem> playlistItems) {
    this.playlistItems = playlistItems;
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
