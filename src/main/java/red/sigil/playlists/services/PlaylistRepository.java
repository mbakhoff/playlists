package red.sigil.playlists.services;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import red.sigil.playlists.entities.Playlist;

import java.util.List;
import java.util.Set;

public interface PlaylistRepository extends CrudRepository<Playlist, Long> {

  List<Playlist> findAllByOrderByLastUpdateAsc(Pageable page);

  Playlist findByYoutubeId(String id);

  List<Playlist> findByYoutubeIdIn(Set<String> ids);
}
