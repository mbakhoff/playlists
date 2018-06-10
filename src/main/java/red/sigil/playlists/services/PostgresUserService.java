package red.sigil.playlists.services;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import red.sigil.playlists.model.Account;

import java.util.Collections;
import java.util.Set;

@Component
@Transactional(rollbackFor = Throwable.class)
public class PostgresUserService implements RegisterableUserService {

  private static final Set<GrantedAuthority> DEFAULT_AUTHORITY = Collections.singleton(
      new SimpleGrantedAuthority("ROLE_USER")
  );

  private final AccountRepository accounts;
  private final PasswordEncoder passwordEncoder;

  public PostgresUserService(AccountRepository accounts, PasswordEncoder passwordEncoder) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Account account = accounts.findByEmail(username.toLowerCase());
    if (account == null)
      throw new UsernameNotFoundException(username);
    return new User(username, account.getPassword(), DEFAULT_AUTHORITY);
  }

  public void register(String username, String password) {
    if (!isValid(username))
      throw new IllegalArgumentException("bad username");
    if (!isValid(password))
      throw new IllegalArgumentException("bad password");

    Account account = accounts.findByEmail(username.toLowerCase());
    if (account != null)
      throw new IllegalStateException("Account already exists: " + username);

    accounts.save(new Account(null, username.toLowerCase(), passwordEncoder.encode(password)));
  }

  private boolean isValid(String value) {
    return value != null && !value.trim().isEmpty() && value.length() < 128;
  }
}
