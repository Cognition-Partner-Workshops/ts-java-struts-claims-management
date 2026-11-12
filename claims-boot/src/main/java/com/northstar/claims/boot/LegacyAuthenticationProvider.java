package com.northstar.claims.boot;

import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/** Legacy LoginAction credential rules: supervisor/supervisor, or adjuster* with legacy*. */
@Component
public class LegacyAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName();
        Object credentials = authentication.getCredentials();
        String password = credentials == null ? null : credentials.toString();
        if (username == null || password == null) {
            throw new BadCredentialsException("login.failed");
        }
        String role;
        if ("supervisor".equals(username) && "supervisor".equals(password)) {
            role = "ROLE_SUPERVISOR";
        } else if (username.startsWith("adjuster") && password.startsWith("legacy")) {
            role = "ROLE_ADJUSTER";
        } else {
            throw new BadCredentialsException("login.failed");
        }
        return UsernamePasswordAuthenticationToken.authenticated(
                username, null, List.of(new SimpleGrantedAuthority(role)));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
