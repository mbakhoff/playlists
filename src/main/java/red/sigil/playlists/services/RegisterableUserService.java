package red.sigil.playlists.services;

import org.springframework.security.core.userdetails.UserDetailsService;

public interface RegisterableUserService extends UserDetailsService {

  void register(String username, String password);
}
