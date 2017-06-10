package red.sigil.playlists.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.services.PlaylistService;
import red.sigil.playlists.TransactionScope;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.List;

@Controller
public class PlaylistsController {
  
  @Autowired
  private PlaylistService playlistService;

  @Autowired
  private DataSource dataSource;

  @RequestMapping(path = "/", method = RequestMethod.GET)
  public String getOverview(@AuthenticationPrincipal User user, ModelMap model) throws Exception {
    try (TransactionScope scope = new TransactionScope(dataSource)) {
      List<Playlist> playlists = playlistService.getPlaylistsByEmail(user.getUsername());
      model.addAttribute("email", user.getUsername());
      model.addAttribute("playlists", playlists);
      scope.commit();
      return "playlists";
    }
  }

  @RequestMapping(path = "/start", method = RequestMethod.POST)
  public String startTracking(@AuthenticationPrincipal User user, @RequestParam("url") String url, ModelMap model) throws Exception {
    try (TransactionScope scope = new TransactionScope(dataSource)) {
      playlistService.startTracking(user.getUsername(), url);
      scope.commit();
    }
    return "redirect:/";
  }

  @RequestMapping(path = "/stop", method = RequestMethod.POST)
  public String stopTracking(@AuthenticationPrincipal User user, @RequestParam MultiValueMap<String, String> params, ModelMap model) throws Exception {
    try (TransactionScope scope = new TransactionScope(dataSource)) {
      List<String> ids = params.get("remove");
      if (ids != null) {
        playlistService.stopTracking(user.getUsername(), new HashSet<>(ids));
      }
      scope.commit();
    }
    return "redirect:/";
  }
}
