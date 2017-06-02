package red.sigil.playlists.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Version;
import java.util.Set;

@Entity
public class Account {

  @Id
  @GeneratedValue
  private Long id;

  @Version
  private Long version;

  private String email;

  private String password;

  @ManyToMany
  private Set<Playlist> playlists;

  public Account() {
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Set<Playlist> getPlaylists() {
    return playlists;
  }

  public void setPlaylists(Set<Playlist> playlists) {
    this.playlists = playlists;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Account account = (Account) o;
    return email != null ? email.equals(account.email) : account.email == null;
  }

  @Override
  public int hashCode() {
    return email != null ? email.hashCode() : 0;
  }
}
