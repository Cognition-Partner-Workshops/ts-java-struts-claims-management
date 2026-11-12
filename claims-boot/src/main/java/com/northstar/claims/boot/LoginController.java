package com.northstar.claims.boot;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {
    @GetMapping("/login.do")
    public String login(@RequestParam(name = "username", defaultValue = "") String username,
                        Model model) {
        model.addAttribute("username", username);
        return "login";
    }
}
