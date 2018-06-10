package red.sigil.playlists.model;

import java.util.Objects;

public class PlaylistItem {

  private Long playlistId;
  private String youtubeId;
  private String title;
  
  public PlaylistItem(Long playlistId, String youtubeId, String title) {
    this.playlistId = playlistId;
    this.youtubeId = youtubeId;
    this.title = title;
  }

  public Long getPlaylistId() {
    return playlistId;
  }

  public void setPlaylistId(Long playlistId) {
    this.playlistId = playlistId;
  }

  public String getYoutubeId() {
    return youtubeId;
  }

  public void setYoutubeId(String youtubeId) {
    this.youtubeId = youtubeId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    PlaylistItem other = (PlaylistItem) o;
    return Objects.equals(playlistId, other.playlistId)
        && Objects.equals(youtubeId, other.youtubeId)
        && Objects.equals(title, other.title);
  }

  @Override
  public int hashCode() {
    return Objects.hash(playlistId, youtubeId);
  }
}
