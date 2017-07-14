package red.sigil.playlists.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.services.AccountRepository;
import red.sigil.playlists.services.PlaylistService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@Transactional
public class PlaylistsController {

  private final AccountRepository accountRepository;
  private final PlaylistService service;

  @Autowired
  public PlaylistsController(AccountRepository accountRepository, PlaylistService service) {
    this.accountRepository = accountRepository;
    this.service = service;
  }

  @RequestMapping(path = "/", method = RequestMethod.GET)
  public String getOverview(@AuthenticationPrincipal User user, ModelMap model) throws Exception {
    Set<Playlist> playlists = accountRepository.findByEmail(user.getUsername()).getPlaylists();
    model.addAttribute("email", user.getUsername());
    model.addAttribute("playlists", playlists);
    return "playlists";
  }

  @RequestMapping(path = "/start", method = RequestMethod.POST)
  public String startTracking(@AuthenticationPrincipal User user, @RequestParam("url") String url, ModelMap model) throws Exception {
    service.startTracking(user.getUsername(), url);
    return "redirect:/";
  }

  @RequestMapping(path = "/stop", method = RequestMethod.POST)
  public String stopTracking(@AuthenticationPrincipal User user, @RequestParam MultiValueMap<String, String> params, ModelMap model) throws Exception {
    List<String> ids = params.get("remove");
    if (ids != null) {
      service.stopTracking(user.getUsername(), new HashSet<>(ids));
    }
    return "redirect:/";
  }
}
