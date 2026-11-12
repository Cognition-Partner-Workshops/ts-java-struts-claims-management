package com.northstar.claims.boot;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Legacy error.jsp behavior. The default Struts module sends unknown actions
 * and action exceptions to error.jsp with status 200 (reference status
 * "null"); the /admin module has neither, so the container answers 500;
 * everything else that is missing is a 404 error page.
 */
@ControllerAdvice
@Controller
public class LegacyErrorHandling implements ErrorController {

    private static final Log log = LogFactory.getLog(LegacyErrorHandling.class);

    static ModelAndView errorPage(Integer status) {
        ModelAndView page = new ModelAndView("error");
        page.addObject("referenceStatus", String.valueOf(status));
        if (status != null) {
            page.setStatus(HttpStatus.valueOf(status));
        }
        return page;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView notFound(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.endsWith(".do")) {
            return errorPage(404);
        }
        return isAdmin(path) ? errorPage(500) : errorPage(null);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView failure(HttpServletRequest request, Exception failure) {
        log.error("Request failed: " + request.getRequestURI(), failure);
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return isAdmin(path) ? errorPage(500) : errorPage(null);
    }

    @RequestMapping("/error")
    public ModelAndView error(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        return errorPage(status instanceof Integer code ? code : 500);
    }

    private static boolean isAdmin(String path) {
        return path.startsWith("/admin/");
    }
}
