package com.northstar.claims.boot;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfiguration {
    @Bean
    FilterRegistrationBean<Filter> authFilter() {
        Filter filter = (request, response, chain) -> {
            HttpServletRequest http = (HttpServletRequest) request;
            String uri = http.getRequestURI();
            var session = http.getSession();
            if (uri.endsWith("login.do") || uri.endsWith("index.jsp")
                    || uri.endsWith(".css") || uri.endsWith(".gif")
                    || uri.endsWith(".jpg") || session.getAttribute("user") != null) {
                chain.doFilter(request, response);
            } else {
                ((HttpServletResponse) response).sendRedirect(http.getContextPath() + "/login.do");
            }
        };
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("*.do");
        return registration;
    }
}
