package red.sigil.playlists.services;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import red.sigil.playlists.entities.Playlist;

import java.util.List;

public interface PlaylistRepository extends CrudRepository<Playlist, Long> {

  List<Playlist> findAllByOrderByLastUpdateDesc(Pageable page);

  Playlist findByYoutubeId(String id);

}
