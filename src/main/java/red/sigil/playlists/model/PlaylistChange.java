package red.sigil.playlists.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import java.time.Instant;

@Entity
public class PlaylistChange {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    private Playlist playlist;

    private String youtubeId;

    private String oldTitle;

    private String newTitle;

    private Instant createdAt;

    public PlaylistChange() {
    }

    public PlaylistChange(Playlist playlist, String youtubeId, String oldTitle, String newTitle) {
        this.playlist = playlist;
        this.youtubeId = youtubeId;
        this.oldTitle = oldTitle;
        this.newTitle = newTitle;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    public String getYoutubeId() {
        return youtubeId;
    }

    public void setYoutubeId(String youtubeId) {
        this.youtubeId = youtubeId;
    }

    public String getOldTitle() {
        return oldTitle;
    }

    public void setOldTitle(String oldTitle) {
        this.oldTitle = oldTitle;
    }

    public String getNewTitle() {
        return newTitle;
    }

    public void setNewTitle(String newTitle) {
        this.newTitle = newTitle;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
