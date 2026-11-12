package com.northstar.claims.boot;

import com.northstar.claims.boot.util.ClaimsConfig;
import java.util.List;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ScreenAdvice {
    @ModelAttribute("asOfDate")
    public String asOfDate() {
        return ClaimsConfig.get("report.asof.date");
    }

    @ModelAttribute("errors")
    public List<Object> errors() {
        return List.of();
    }
}
