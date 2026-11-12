package com.northstar.claims.boot;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Ports LoginAction, LogoutAction and the login.do welcome file. */
@Controller
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy contextHolder =
            SecurityContextHolder.getContextHolderStrategy();
    private final SessionAuthenticationStrategy sessionStrategy =
            new ChangeSessionIdAuthenticationStrategy();
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public LoginController(AuthenticationManager authenticationManager,
                           SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        logoutHandler.setSecurityContextRepository(securityContextRepository);
    }

    @GetMapping("/")
    public String welcome() {
        return "forward:/login.do";
    }

    @RequestMapping("/login.do")
    public String login(@RequestParam(name = "username", required = false) String username,
                        @RequestParam(name = "password", required = false) String password,
                        HttpServletRequest request, HttpServletResponse response, Model model) {
        model.addAttribute("username", username == null ? "" : username);
        request.setAttribute("screenName", "detail");
        if (username != null && password != null) {
            try {
                Authentication authentication = authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(username, password));
                signIn(authentication, request, response);
                HttpSession session = request.getSession();
                session.setAttribute("user", username);
                session.setAttribute("displayName", username);
                request.setAttribute("loginStatus", "AUTHENTICATED");
                model.addAttribute("user", username);
                return "home";
            } catch (AuthenticationException rejected) {
                // fall through to the legacy rejected login screen
            }
        }
        request.setAttribute("message", "login.failed");
        request.setAttribute("loginStatus", "REJECTED");
        return "login";
    }

    @RequestMapping("/logout.do")
    public String logout(HttpServletRequest request, HttpServletResponse response, Model model) {
        logoutHandler.logout(request, response, contextHolder.getContext().getAuthentication());
        model.addAttribute("username", "");
        return "login";
    }

    private void signIn(Authentication authentication, HttpServletRequest request,
                        HttpServletResponse response) {
        sessionStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
