package red.sigil.playlists.services;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import red.sigil.playlists.model.Account;
import red.sigil.playlists.model.Playlist;
import red.sigil.playlists.model.PlaylistSubscription;

import java.util.List;

@Component
public interface PlaylistSubscriptionRepository extends CrudRepository<PlaylistSubscription, Long> {

  PlaylistSubscription findByAccountAndPlaylist(Account account, Playlist playlist);

  List<PlaylistSubscription> findByPlaylist(Playlist playlist);
}
