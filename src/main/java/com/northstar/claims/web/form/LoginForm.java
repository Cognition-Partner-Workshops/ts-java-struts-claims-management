package com.northstar.claims.web.form;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * Form bean for the LoginForm screen.
 * String properties preserve the request-bound behavior of the original UI.
 */
public class LoginForm extends BaseForm {

    private String username;
    private String password;

    /** Creates an empty form for the request scope. */
    public LoginForm() {
        super();
    }

    /** Returns the username submitted by the browser. */
    public String getUsername() {
        return username;
    }

    /** Stores the username submitted by the browser. */
    public void setUsername(String username) {
        this.username = username;
    }

    /** Returns the password submitted by the browser. */
    public String getPassword() {
        return password;
    }

    /** Stores the password submitted by the browser. */
    public void setPassword(String password) {
        this.password = password;
    }

    /** Clears fields before Struts reuses the form instance. */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        username = null;
        password = null;
    }

    /** Performs the screen-specific validation hook. */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        return super.validate(mapping, request);
    }
}
