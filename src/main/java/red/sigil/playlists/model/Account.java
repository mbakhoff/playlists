package red.sigil.playlists.model;

import java.util.Objects;

public class Account {

  private Long id;

  private String email;

  private String password;

  public Account(Long id, String email, String password) {
    this.id = id;
    this.email = email;
    this.password = password;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Account other = (Account) o;
    return Objects.equals(email, other.email);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(email);
  }
}
