package red.sigil.playlists.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

@Component
public class PostgresUserService implements UserDetailsService {

  private static final Set<GrantedAuthority> DEFAULT_AUTHORITY = Collections.singleton(
      new SimpleGrantedAuthority("USER")
  );

  @Autowired
  private DataSource dataSource;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    try (Connection connection = dataSource.getConnection()) {
      PreparedStatement stmt = connection.prepareStatement("SELECT * FROM account WHERE email = ?;");
      stmt.setString(1, username);
      ResultSet rs = stmt.executeQuery();
      if (!rs.next())
        throw new UsernameNotFoundException(username);
      return new User(username, rs.getString("password"), DEFAULT_AUTHORITY);
    } catch (SQLException e) {
      throw new AuthenticationServiceException("Failed to load user " + username, e);
    }
  }

  public void register(String username, String password) {
    if (username == null || username.isEmpty() || username.length() > 128)
      throw new IllegalArgumentException("bad username");
    if (password == null || password.isEmpty() || password.length() > 128)
      throw new IllegalArgumentException("bad password");
    
    try (Connection connection = dataSource.getConnection()) {
      PreparedStatement registration = connection.prepareStatement("INSERT INTO account (email, password) VALUES (?, ?);");
      registration.setString(1, username);
      registration.setString(2, passwordEncoder.encode(password));
      if (registration.executeUpdate() != 1)
        throw new IllegalStateException("Registration failed for " + username);
    } catch (SQLException e) {
      throw new IllegalStateException("Auth database error", e);
    }
  }
}
