package red.sigil.playlists.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import red.sigil.playlists.services.RegisterableUserService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@Controller
@Transactional
public class LoginController {

  private final RegisterableUserService userService;

  @Autowired
  public LoginController(RegisterableUserService userService) {
    this.userService = userService;
  }

  @GetMapping("/auth/login")
  public String renderLogin() {
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
}
