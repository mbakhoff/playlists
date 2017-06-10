package red.sigil.playlists.entities;

public class PlaylistItem {

  private Long id;
  private String youtubeId;
  private String title;

  public PlaylistItem(Long id, String youtubeId, String title) {
    this.id = id;
    this.youtubeId = youtubeId;
    this.title = title;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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
    PlaylistItem that = (PlaylistItem) o;
    return youtubeId != null ? youtubeId.equals(that.youtubeId) : that.youtubeId == null;
  }

  @Override
  public int hashCode() {
    return youtubeId != null ? youtubeId.hashCode() : 0;
  }
}
