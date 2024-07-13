package red.sigil.playlists.services;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import red.sigil.playlists.model.Account;

@Component
public interface AccountRepository extends CrudRepository<Account, Long> {

  Account findByName(String name);
}
