package red.sigil.playlists.services;

import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import red.sigil.playlists.entities.Account;

import java.util.List;

public interface AccountRepository extends SqlObject {

  @SqlQuery("SELECT * FROM account WHERE email=?")
  Account findByEmail(String email);

  @SqlQuery("SELECT * FROM account a JOIN account_playlists ap ON a.id = ap.accounts_id WHERE ap.playlists_id = :id")
  List<Account> findByPlaylist(long id);

  default void save(Account account) {
    long id = getHandle()
        .createUpdate("INSERT INTO account (id, email, password) VALUES (nextval('hibernate_sequence'), :email, :password)")
        .bindBean(account)
        .executeAndReturnGeneratedKeys("id")
        .mapTo(long.class)
        .findOnly();
    account.setId(id);
  }
}
