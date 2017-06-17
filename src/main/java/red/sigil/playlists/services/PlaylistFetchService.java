package red.sigil.playlists.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PlaylistFetchService {

  private final PropertyService propertyService;

  private static final String PLAYLISTS =
      "https://www.googleapis.com/youtube/v3/playlists";
  private static final String PLAYLIST_ITEMS =
      "https://www.googleapis.com/youtube/v3/playlistItems";

  private CloseableHttpClient httpclient;
  private ObjectMapper mapper;
  private String apiKey;

  @Autowired
  public PlaylistFetchService(PropertyService propertyService) {
    this.propertyService = propertyService;
  }

  @PostConstruct
  public void init() throws Exception {
    apiKey = propertyService.getProperty("yt-apikey");
    httpclient = HttpClients.createDefault();
    mapper = new ObjectMapper();
  }

  @PreDestroy
  public void destroy() throws Exception {
    httpclient.close();
  }

  public ItemInfo read(String playlistId) throws Exception {
    HttpGet httpGet = new HttpGet(new URIBuilder(PLAYLISTS)
        .addParameter("part", "id,snippet")
        .addParameter("id", playlistId)
        .addParameter("key", apiKey)
        .build());

    try (CloseableHttpResponse resp = httpclient.execute(httpGet)) {
      StatusLine status = resp.getStatusLine();
      if (status.getStatusCode() != 200)
        throw new IOException(status.getReasonPhrase());

      JsonNode root = mapper.readTree(resp.getEntity().getContent());
      JsonNode items = root.at("/items");
      if (items.size() == 0)
        return null;

      JsonNode playlist = items.get(0);
      return new ItemInfo(
          playlist.at("/id").asText(),
          playlist.at("/snippet/title").asText());
    }
  }

  public List<ItemInfo> readItems(String playlistId) throws Exception {
    List<ItemInfo> playlist = new ArrayList<>();
    String page = null;
    do {
      HttpGet httpGet = new HttpGet(new URIBuilder(PLAYLIST_ITEMS)
          .addParameter("part", "id,snippet,contentDetails")
          .addParameter("playlistId", playlistId)
          .addParameter("maxResults", "50")
          .addParameter("pageToken", page)
          .addParameter("key", apiKey)
          .build());

      try (CloseableHttpResponse resp = httpclient.execute(httpGet)) {
        StatusLine status = resp.getStatusLine();
        if (status.getStatusCode() != 200)
          throw new IOException(status.getReasonPhrase());

        JsonNode root = mapper.readTree(resp.getEntity().getContent());
        page = root.at("/nextPageToken").asText(null);
        for (JsonNode playlistItem : root.at("/items")) {
          playlist.add(new ItemInfo(
              playlistItem.at("/contentDetails/videoId").asText(),
              playlistItem.at("/snippet/title").asText()));
        }
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
  }
}
