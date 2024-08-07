package red.sigil.playlists.services;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import red.sigil.playlists.model.Playlist;

import java.util.List;

@Component
public interface PlaylistRepository extends CrudRepository<Playlist, Long> {

  List<Playlist> findAllTop100ByOrderByLastUpdateAsc();

  Playlist findByYoutubeId(String id);
}
