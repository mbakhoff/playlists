package red.sigil.playlists.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import red.sigil.playlists.services.RegisterableUserService;

@Controller
@Transactional(rollbackFor = Throwable.class)
public class LoginController {

  private final RegisterableUserService userService;

  @Autowired
  public LoginController(RegisterableUserService userService) {
    this.userService = userService;
  }

  @GetMapping("/auth/login")
  public String renderLogin() {
    if (isAuthenticated()) {
      return "redirect:/";
    }
    return "login";
  }

  @PostMapping("/auth/signup")
  public String processSignup(HttpServletRequest request,
                              @RequestParam("username") String username,
                              @RequestParam("password") String password) throws ServletException {
    userService.register(username, password);
    request.login(username, password);
    return "redirect:/";
  }

  private boolean isAuthenticated() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && !(auth instanceof AnonymousAuthenticationToken);
  }
}
