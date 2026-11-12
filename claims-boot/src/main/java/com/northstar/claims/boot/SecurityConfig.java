package com.northstar.claims.boot;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Replaces the legacy AuthFilter: every {@code *.do} request except
 * {@code login.do} needs a signed-in session, otherwise it is redirected to
 * {@code /login.do}. Login and logout are handled by controllers so the
 * legacy forwards (home or login page, no redirect) are preserved.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            SecurityContextRepository securityContextRepository) throws Exception {
        http
            // Legacy forms and replay post without tokens.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                .requestMatchers(SecurityConfig::isLogin).permitAll()
                .requestMatchers(SecurityConfig::isAction).authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, failure) ->
                response.sendRedirect(request.getContextPath() + "/login.do")))
            .securityContext(ctx -> ctx.securityContextRepository(securityContextRepository))
            .requestCache(cache -> cache.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable());
        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(LegacyAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /** Legacy Jetty sent absolute Location headers. */
    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> absoluteRedirects() {
        return factory -> factory.addContextCustomizers(context -> context.setUseRelativeRedirects(false));
    }

    static boolean isLogin(HttpServletRequest request) {
        return request.getRequestURI().endsWith("login.do");
    }

    static boolean isAction(HttpServletRequest request) {
        return request.getRequestURI().endsWith(".do");
    }
}
