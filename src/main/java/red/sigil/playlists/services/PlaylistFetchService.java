package red.sigil.playlists.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class PlaylistFetchService {

  private final Environment env;

  private static final String PLAYLISTS =
      "https://www.googleapis.com/youtube/v3/playlists";
  private static final String PLAYLIST_ITEMS =
      "https://www.googleapis.com/youtube/v3/playlistItems";

  private HttpClient httpclient;
  private ObjectMapper mapper;
  private String apiKey;

  public PlaylistFetchService(Environment env) {
    this.env = env;
  }

  @PostConstruct
  public void init() {
    apiKey = env.getProperty("yt-apikey");
    httpclient = HttpClient.newHttpClient();
    mapper = new ObjectMapper();
  }

  public ItemInfo read(String playlistId) throws Exception {
    HttpRequest httpGet = HttpRequest.newBuilder(UriComponentsBuilder.fromUriString(PLAYLISTS)
        .queryParam("part", "id,snippet")
        .queryParam("id", playlistId)
        .queryParam("key", apiKey)
        .build().toUri()).GET().build();

    HttpResponse<String> resp = httpclient.send(httpGet, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() != 200)
      throw new IOException(resp.statusCode() + " " + resp.body());

    JsonNode root = mapper.readTree(resp.body());
    JsonNode items = root.at("/items");
    if (items.size() == 0)
      throw new PlaylistNotFound(playlistId);

    JsonNode playlist = items.get(0);
    return new ItemInfo(
            playlist.at("/id").asText(),
            playlist.at("/snippet/title").asText());
  }

  public List<ItemInfo> readItems(String playlistId) throws Exception {
    List<ItemInfo> playlist = new ArrayList<>();
    String page = null;
    do {
      HttpRequest httpGet = HttpRequest.newBuilder(UriComponentsBuilder.fromUriString(PLAYLIST_ITEMS)
          .queryParam("part", "id,snippet,contentDetails")
          .queryParam("playlistId", playlistId)
          .queryParam("maxResults", "50")
          .queryParam("pageToken", page)
          .queryParam("key", apiKey)
          .build().toUri()).GET().build();

      HttpResponse<String> resp = httpclient.send(httpGet, HttpResponse.BodyHandlers.ofString());
      if (resp.statusCode() != 200)
        throw new IOException(resp.statusCode() + " " + resp.body());

      JsonNode root = mapper.readTree(resp.body());
      page = root.at("/nextPageToken").asText(null);
      for (JsonNode playlistItem : root.at("/items")) {
        ItemInfo info = new ItemInfo(
                playlistItem.at("/contentDetails/videoId").asText(),
                playlistItem.at("/snippet/title").asText());
        if ("Deleted video".equals(info.title) || "Private video".equals(info.title))
          continue;
        playlist.add(info);
      }
    } while (page != null);
    return playlist;
  }

  public static class ItemInfo {

    public final String id;
    public final String title;

    public ItemInfo(String id, String title) {
      this.id = id;
      this.title = title;
    }

    @Override
    public String toString() {
      return String.format("ItemInfo{id='%s', title='%s'}", id, title);
    }
  }

  public static class PlaylistNotFound extends RuntimeException {
    public PlaylistNotFound(String message) {
      super(message);
    }
  }
}
