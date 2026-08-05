package com.northstar.claims.web.form;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * Form bean for the AdjusterForm screen.
 * String properties preserve the request-bound behavior of the original UI.
 */
public class AdjusterForm extends BaseForm {

    private String fullName;
    private String region;
    private String active;

    /** Creates an empty form for the request scope. */
    public AdjusterForm() {
        super();
    }

    /** Returns the fullName submitted by the browser. */
    public String getFullName() {
        return fullName;
    }

    /** Stores the fullName submitted by the browser. */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /** Returns the region submitted by the browser. */
    public String getRegion() {
        return region;
    }

    /** Stores the region submitted by the browser. */
    public void setRegion(String region) {
        this.region = region;
    }

    /** Returns the active submitted by the browser. */
    public String getActive() {
        return active;
    }

    /** Stores the active submitted by the browser. */
    public void setActive(String active) {
        this.active = active;
    }

    /** Clears fields before Struts reuses the form instance. */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        fullName = null;
        region = null;
        active = null;
    }

    /** Performs the screen-specific validation hook. */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        return super.validate(mapping, request);
    }
}
