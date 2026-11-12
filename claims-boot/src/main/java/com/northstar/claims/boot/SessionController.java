package com.northstar.claims.boot;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SessionController {
    @RequestMapping("/login.do")
    public String login(HttpServletRequest request, Model model) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        boolean valid = username != null && password != null
                && (("supervisor".equals(username) && "supervisor".equals(password))
                    || (username.startsWith("adjuster") && password.startsWith("legacy")));
        if (valid) {
            request.getSession().setAttribute("user", username);
            request.getSession().setAttribute("displayName", username);
            model.addAttribute("loginStatus", "AUTHENTICATED");
            model.addAttribute("screenName", "detail");
            return "home";
        }
        model.addAttribute("message", "login.failed");
        model.addAttribute("loginStatus", "REJECTED");
        model.addAttribute("screenName", "detail");
        model.addAttribute("username", username);
        model.addAttribute("password", password);
        return "login";
    }

    @RequestMapping("/home.do")
    public String home(Model model) {
        model.addAttribute("screenName", "home");
        model.addAttribute("screenMode", "read");
        model.addAttribute("operatorScope", "claims");
        model.addAttribute("homeArea", "claims operations");
        model.addAttribute("homeNavigation", "standard");
        model.addAttribute("homeStatus", "available");
        model.addAttribute("homeFormat", "html");
        model.addAttribute("homeRole", "operator");
        model.addAttribute("homeVersion", "claims");
        return "home";
    }

    @RequestMapping("/logout.do")
    public String logout(HttpServletRequest request, Model model) {
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }
        model.addAttribute("logoutStatus", "COMPLETE");
        model.addAttribute("screenName", "detail");
        return "login";
    }
}
