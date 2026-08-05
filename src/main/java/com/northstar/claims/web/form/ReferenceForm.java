package com.northstar.claims.web.form;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * Form bean for the ReferenceForm screen.
 * String properties preserve the request-bound behavior of the original UI.
 */
public class ReferenceForm extends BaseForm {


    /** Creates an empty form for the request scope. */
    public ReferenceForm() {
        super();
    }

    /** Clears fields before Struts reuses the form instance. */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
    }

    /** Performs the screen-specific validation hook. */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        return super.validate(mapping, request);
    }
}
