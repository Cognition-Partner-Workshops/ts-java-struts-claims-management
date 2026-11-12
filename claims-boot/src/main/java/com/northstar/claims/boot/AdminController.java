package com.northstar.claims.boot;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * The legacy /admin Struts module as it behaves today (ROUTES.md): both actions
 * run, but their module-relative forwards miss, so the container answers 404.
 * Unmapped /admin actions (e.g. the linked editAdjuster.do) fail with 500; see
 * {@link LegacyErrorHandling}.
 */
@Controller
public class AdminController {

    private final JdbcTemplate jdbc;

    public AdminController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @RequestMapping("/admin/adjusters.do")
    public ModelAndView adjusters() {
        jdbc.queryForList("select * from ADJUSTER order by adjuster_id");
        return LegacyErrorHandling.errorPage(404);
    }

    @RequestMapping("/admin/reference.do")
    public ModelAndView reference() {
        return LegacyErrorHandling.errorPage(404);
    }
}
