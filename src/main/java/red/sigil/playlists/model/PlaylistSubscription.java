package red.sigil.playlists.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

@Entity
public class PlaylistSubscription {

  @Id
  @GeneratedValue
  private Long id;

  @Version
  private Long version;

  @OneToOne
  private Playlist playlist;

  @OneToOne
  private Account account;

  private Long lastChange;

  public PlaylistSubscription() {
  }

  public PlaylistSubscription(Playlist playlist, Account account) {
    this.playlist = playlist;
    this.account = account;
    this.lastChange = 0L;
  }

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

  public Playlist getPlaylist() {
    return playlist;
  }

  public void setPlaylist(Playlist playlist) {
    this.playlist = playlist;
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  public Long getLastChange() {
    return lastChange;
  }

  public void setLastChange(Long lastChange) {
    this.lastChange = lastChange;
  }
}
