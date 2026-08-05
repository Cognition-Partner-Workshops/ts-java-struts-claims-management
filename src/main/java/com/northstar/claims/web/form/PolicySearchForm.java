package com.northstar.claims.web.form;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * Form bean for the PolicySearchForm screen.
 * String properties preserve the request-bound behavior of the original UI.
 */
public class PolicySearchForm extends BaseForm {

    private String lineOfBusiness;

    /** Creates an empty form for the request scope. */
    public PolicySearchForm() {
        super();
    }

    /** Returns the lineOfBusiness submitted by the browser. */
    public String getLineOfBusiness() {
        return lineOfBusiness;
    }

    /** Stores the lineOfBusiness submitted by the browser. */
    public void setLineOfBusiness(String lineOfBusiness) {
        this.lineOfBusiness = lineOfBusiness;
    }

    /** Clears fields before Struts reuses the form instance. */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        lineOfBusiness = null;
    }

    /** Performs the screen-specific validation hook. */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        return super.validate(mapping, request);
    }
}
