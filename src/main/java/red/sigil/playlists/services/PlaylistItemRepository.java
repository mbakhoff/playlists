package red.sigil.playlists.services;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import red.sigil.playlists.model.Playlist;
import red.sigil.playlists.model.PlaylistItem;

import java.util.List;

@Component
public interface PlaylistItemRepository extends CrudRepository<PlaylistItem, Long> {

    List<PlaylistItem> findByPlaylist(Playlist playlist);
}
