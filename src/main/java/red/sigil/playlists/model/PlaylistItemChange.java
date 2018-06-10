package red.sigil.playlists.model;

public class PlaylistItemChange {

  private final String playlistItem;
  private final String oldTitle;
  private final String newTitle;

  public PlaylistItemChange(String playlistItem, String oldTitle, String newTitle) {
    this.playlistItem = playlistItem;
    this.oldTitle = oldTitle;
    this.newTitle = newTitle;
  }

  public String getPlaylistItem() {
    return playlistItem;
  }

  public String getOldTitle() {
    return oldTitle;
  }

  public String getNewTitle() {
    return newTitle;
  }

  @Override
  public String toString() {
    return "PlaylistItemChange{" +
        "playlistItem='" + playlistItem + '\'' +
        ", oldTitle='" + oldTitle + '\'' +
        ", newTitle='" + newTitle + '\'' +
        '}';
  }
}
