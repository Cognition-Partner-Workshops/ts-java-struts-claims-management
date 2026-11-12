package com.northstar.claims.boot;

import com.northstar.claims.boot.util.ClaimsConfig;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class ScreenAdvice {
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String missingResource(Model model) {
        model.addAttribute("status", 404);
        return "error";
    }

    @ModelAttribute("asOfDate")
    public String asOfDate() {
        return ClaimsConfig.get("report.asof.date");
    }

    @ModelAttribute("errors")
    public List<Object> errors() {
        return List.of();
    }
}
