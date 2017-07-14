package red.sigil.playlists.services;

import org.springframework.data.repository.CrudRepository;
import red.sigil.playlists.entities.Account;

public interface AccountRepository extends CrudRepository<Account, Long> {

  Account findByEmail(String email);

}
