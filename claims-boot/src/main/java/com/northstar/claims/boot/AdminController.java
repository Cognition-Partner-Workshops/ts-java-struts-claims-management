package com.northstar.claims.boot;

import com.northstar.claims.boot.dao.AdjusterDAO;
import java.sql.SQLException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class AdminController {
    private final AdjusterDAO adjusters;

    public AdminController(AdjusterDAO adjusters) {
        this.adjusters = adjusters;
    }

    @RequestMapping("/admin/adjusters.do")
    public String adjusters(Model model) throws SQLException {
        List rows = adjusters.findAll();
        model.addAttribute("adjusters", rows);
        model.addAttribute("adjusterCount", rows.size());
        return "forward:/admin/WEB-INF/jsp/admin/adjusters.jsp";
    }

    @RequestMapping("/admin/reference.do")
    public String reference(Model model) {
        model.addAttribute("referenceVersion", "2019.04");
        model.addAttribute("lossTypes", List.of("WATER", "FIRE", "THEFT", "COLLISION"));
        return "forward:/admin/WEB-INF/jsp/admin/reference.jsp";
    }

    @RequestMapping("/admin/editAdjuster.do")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String editAdjuster(Model model) {
        model.addAttribute("status", 500);
        return "error";
    }
}
