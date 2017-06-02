package red.sigil.playlists.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import red.sigil.playlists.services.PostgresUserService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@Controller
public class LoginController {

  @Autowired
  private PostgresUserService userService;

  @RequestMapping(path = "/auth/login", method = RequestMethod.GET)
  public String renderLogin() {
    return "login";
  }

  @RequestMapping(path = "/auth/signup", method = RequestMethod.POST)
  public String processSignup(HttpServletRequest request,
                              @RequestParam("username") String username,
                              @RequestParam("password") String password) throws ServletException {
    userService.register(username, password);
    request.login(username, password);
    return "redirect:/";
  }
}
