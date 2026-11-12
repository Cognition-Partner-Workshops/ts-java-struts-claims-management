package com.northstar.claims.boot;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlaceholderController {

    @GetMapping(value = "/login.do", produces = "text/plain")
    public String login() {
        return "Claims Boot foundation: login screen not implemented yet.\n";
    }
}
