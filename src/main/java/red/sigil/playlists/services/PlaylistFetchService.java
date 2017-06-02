package red.sigil.playlists.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class PlaylistFetchService {

  @Autowired
  private PropertyService propertyService;

  private YouTube youtube;
  private String apiKey;

  @PostConstruct
  public void init() throws Exception {
    apiKey = propertyService.getProperty("yt-apikey");
    youtube = new YouTube(
        GoogleNetHttpTransport.newTrustedTransport(),
        JacksonFactory.getDefaultInstance(),
        req -> { });
  }

  public ItemInfo read(String playlistId) throws Exception {
    List<Playlist> items = youtube.playlists().list("id,snippet")
        .setKey(apiKey)
        .setId(playlistId)
        .execute()
        .getItems();
    return items.isEmpty() ? null : new ItemInfo(items.get(0));
  }

  public List<ItemInfo> readItems(String playlistId) throws Exception {
    List<ItemInfo> playlist = new ArrayList<>();
    String page = null;
    do {
      PlaylistItemListResponse result = youtube.playlistItems().list("id,snippet,contentDetails")
          .setKey(apiKey)
          .setPlaylistId(playlistId)
          .setMaxResults(50L)
          .setPageToken(page)
          .execute();
      for (PlaylistItem item : result.getItems()) {
        playlist.add(new ItemInfo(item));
      }
      page = result.getNextPageToken();
    } while (page != null);
    return playlist;
  }

  public static class ItemInfo {
    public final String id;
    public final String title;

    public ItemInfo(Playlist playlist) {
      this.id = playlist.getId();
      this.title = playlist.getSnippet().getTitle();
    }

    public ItemInfo(PlaylistItem item) {
      this.id = item.getContentDetails().getVideoId();
      this.title = item.getSnippet().getTitle();
    }
  }
}
