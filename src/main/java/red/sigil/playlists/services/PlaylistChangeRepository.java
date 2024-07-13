package red.sigil.playlists.services;

import org.springframework.data.repository.CrudRepository;
import red.sigil.playlists.model.Playlist;
import red.sigil.playlists.model.PlaylistChange;

import java.util.List;

public interface PlaylistChangeRepository extends CrudRepository<PlaylistChange, Long> {

  List<PlaylistChange> findByPlaylistAndIdGreaterThanOrderByIdAsc(Playlist playlist, long id);
}
