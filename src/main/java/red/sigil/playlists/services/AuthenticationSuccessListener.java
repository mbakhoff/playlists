package red.sigil.playlists.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import red.sigil.playlists.model.Account;

@Component
public class AuthenticationSuccessListener {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationSuccessListener.class);

  @Autowired
  private AccountRepository accounts;

  @Transactional
  @EventListener(classes = InteractiveAuthenticationSuccessEvent.class)
  public void ensureAccountCreated(InteractiveAuthenticationSuccessEvent event) {
    if (event.getAuthentication().getPrincipal() instanceof OAuth2User user) {
      String name = user.getName();
      String email = user.getAttribute("email");
      if (name == null || name.isEmpty())
        throw new IllegalArgumentException("invalid user id");
      if (email == null || !email.contains("@"))
        throw new IllegalArgumentException("invalid email");

      Account existing = accounts.findByName(name);
      if (existing == null) {
        var account = new Account();
        account.setName(name);
        account.setEmail(email);
        accounts.save(account);
        log.info("created account " + name + " " + email);
      }
    }
  }
}
