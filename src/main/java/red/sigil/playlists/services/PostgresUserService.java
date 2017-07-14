package red.sigil.playlists.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import red.sigil.playlists.entities.Account;

import java.util.Collections;
import java.util.Set;

@Component
public class PostgresUserService implements UserDetailsService {

  private static final Set<GrantedAuthority> DEFAULT_AUTHORITY = Collections.singleton(
      new SimpleGrantedAuthority("USER")
  );

  private final AccountRepository accounts;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  public PostgresUserService(AccountRepository accounts, PasswordEncoder passwordEncoder) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    String password = accounts.findByEmail(username.toLowerCase()).getPassword();
    if (password == null)
      throw new UsernameNotFoundException(username);
    return new User(username, password, DEFAULT_AUTHORITY);
  }

  public void register(String username, String password) {
    if (username == null || username.isEmpty() || username.length() > 128)
      throw new IllegalArgumentException("bad username");
    if (password == null || password.isEmpty() || password.length() > 128)
      throw new IllegalArgumentException("bad password");

    Account account = accounts.findByEmail(username.toLowerCase());
    if (account != null)
      throw new IllegalStateException("Account already exists: " + username);

    accounts.save(new Account(null, username.toLowerCase(), passwordEncoder.encode(password)));
  }
}
