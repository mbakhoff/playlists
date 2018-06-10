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
import red.sigil.playlists.model.Account;
import red.sigil.playlists.model.Playlist;
import red.sigil.playlists.services.AccountRepository;
import red.sigil.playlists.services.PlaylistRepository;
import red.sigil.playlists.services.PlaylistService;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@Transactional(rollbackFor = Throwable.class)
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
  public String getOverview(@AuthenticationPrincipal User user, ModelMap model) {
    Account account = accountRepository.findByEmail(user.getUsername());
    List<Playlist> playlists = playlistRepository.findAllByAccount(account.getId());
    model.addAttribute("email", user.getUsername());
    model.addAttribute("playlists", playlists);
    return "playlists";
  }

  @PostMapping("/start")
  public String startTracking(@AuthenticationPrincipal User user, @RequestParam("url") String url) {
    service.startTracking(user.getUsername(), parseListId(url));
    return "redirect:/";
  }

  @PostMapping("/stop")
  public String stopTracking(@AuthenticationPrincipal User user, @RequestParam MultiValueMap<String, String> params) {
    List<String> ids = params.get("remove");
    if (ids != null) {
      for (String id : ids) {
        service.stopTracking(user.getUsername(), id);
      }
    }
    return "redirect:/";
  }

  private String parseListId(String url) {
    // e.g. https://www.youtube.com/playlist?list=PL7USMo--IcSi5p44jTZTezqS3Z-MEC6DU
    Matcher matcher = Pattern.compile(".*[?&]list=([A-Za-z0-9\\-_]+).*").matcher(url);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    throw new IllegalArgumentException(url);
  }
}
