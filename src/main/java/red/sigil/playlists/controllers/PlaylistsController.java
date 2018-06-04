package red.sigil.playlists.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import red.sigil.playlists.entities.Account;
import red.sigil.playlists.entities.Playlist;
import red.sigil.playlists.services.AccountRepository;
import red.sigil.playlists.services.PlaylistRepository;
import red.sigil.playlists.services.PlaylistService;

import java.util.HashSet;
import java.util.List;

@Controller
@Transactional
public class PlaylistsController {

  private final AccountRepository accountRepository;
  private final PlaylistRepository playlistRepository;
  private final PlaylistService service;

  @Autowired
  public PlaylistsController(AccountRepository accountRepository, PlaylistRepository playlistRepository, PlaylistService service) {
    this.accountRepository = accountRepository;
    this.playlistRepository = playlistRepository;
    this.service = service;
  }

  @GetMapping("/")
  public String getOverview(@AuthenticationPrincipal User user, ModelMap model) throws Exception {
    Account account = accountRepository.findByEmail(user.getUsername());
    List<Playlist> playlists = playlistRepository.findAllByAccount(account.getId());
    model.addAttribute("email", user.getUsername());
    model.addAttribute("playlists", playlists);
    return "playlists";
  }

  @PostMapping("/start")
  public String startTracking(@AuthenticationPrincipal User user, @RequestParam("url") String url, ModelMap model) throws Exception {
    service.startTracking(user.getUsername(), url);
    return "redirect:/";
  }

  @PostMapping("/stop")
  public String stopTracking(@AuthenticationPrincipal User user, @RequestParam MultiValueMap<String, String> params, ModelMap model) throws Exception {
    List<String> ids = params.get("remove");
    if (ids != null) {
      service.stopTracking(user.getUsername(), new HashSet<>(ids));
    }
    return "redirect:/";
  }
}
