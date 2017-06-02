package red.sigil.playlists.entities;

import javax.persistence.Embeddable;

@Embeddable
public class PlaylistItem {

  private String youtubeId;

  private String title;

  public PlaylistItem() {
  }

  public PlaylistItem(String youtubeId, String title) {
    this.youtubeId = youtubeId;
    this.title = title;
  }

  public String getYoutubeId() {
    return youtubeId;
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
