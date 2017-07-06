package red.sigil.playlists.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
public class PostgresUserService implements UserDetailsService {

  private static final Set<GrantedAuthority> DEFAULT_AUTHORITY = Collections.singleton(
      new SimpleGrantedAuthority("USER")
  );

  private final JdbcTemplate jdbc;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  public PostgresUserService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
    this.jdbc = jdbc;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    String password = jdbc.queryForObject(
        "SELECT password FROM account WHERE email = ?;",
        String.class,
        username.toLowerCase()
    );

    if (password == null)
      throw new UsernameNotFoundException(username);
    return new User(username, password, DEFAULT_AUTHORITY);
  }

  public void register(String username, String password) {
    if (username == null || username.isEmpty() || username.length() > 128)
      throw new IllegalArgumentException("bad username");
    if (password == null || password.isEmpty() || password.length() > 128)
      throw new IllegalArgumentException("bad password");

    int rows = jdbc.update(
        "INSERT INTO account (email, password) VALUES (?, ?);",
        username.toLowerCase(),
        passwordEncoder.encode(password)
    );
    if (rows != 1)
      throw new IllegalStateException("Registration failed for " + username);
  }
}
