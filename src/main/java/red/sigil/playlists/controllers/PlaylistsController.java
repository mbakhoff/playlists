package red.sigil.playlists.controllers;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import red.sigil.playlists.model.Account;
import red.sigil.playlists.model.Playlist;
import red.sigil.playlists.services.AccountRepository;
import red.sigil.playlists.services.PlaylistRepository;
import red.sigil.playlists.services.PlaylistService;

import java.util.List;

@Controller
@Transactional(rollbackFor = Throwable.class)
public class PlaylistsController {

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private PlaylistRepository playlistRepository;

  @Autowired
  private PlaylistService service;

  @Autowired
  private EntityManager entityManager;

  @GetMapping("/")
  public String getOverview(@AuthenticationPrincipal OAuth2User user, ModelMap model) {
    Account account = accountRepository.findByName(user.getName());
    List<Playlist> playlists = entityManager
        .createQuery("select p from PlaylistSubscription s join s.playlist p where s.account = :account", Playlist.class)
        .setParameter("account", account)
        .getResultList();
    model.addAttribute("email", account.getEmail());
    model.addAttribute("playlists", playlists);
    return "playlists";
  }

  @PostMapping("/start")
  public String startTracking(@AuthenticationPrincipal OAuth2User user, @RequestParam("url") String url) {
    service.startTracking(user.getName(), parseListId(url));
    return "redirect:/";
  }

  @PostMapping("/stop")
  public String stopTracking(@AuthenticationPrincipal OAuth2User user, @RequestParam MultiValueMap<String, String> params) {
    List<String> ids = params.get("remove");
    if (ids != null) {
      for (String id : ids) {
        service.stopTracking(user.getName(), id);
      }
    }
    return "redirect:/";
  }

  private String parseListId(String url) {
    // e.g. https://www.youtube.com/playlist?list=PL7USMo--IcSi5p44jTZTezqS3Z-MEC6DU
    var value = UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("list");
    if (value == null)
      throw new IllegalArgumentException("missing list param");
    return value;
  }
}
