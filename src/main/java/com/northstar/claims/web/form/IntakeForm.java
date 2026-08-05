package com.northstar.claims.web.form;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * Form bean for the IntakeForm screen.
 * String properties preserve the request-bound behavior of the original UI.
 */
public class IntakeForm extends BaseForm {

    private String claimantName;
    private String lossDate;
    private String description;

    /** Creates an empty form for the request scope. */
    public IntakeForm() {
        super();
    }

    /** Returns the claimantName submitted by the browser. */
    public String getClaimantName() {
        return claimantName;
    }

    /** Stores the claimantName submitted by the browser. */
    public void setClaimantName(String claimantName) {
        this.claimantName = claimantName;
    }

    /** Returns the lossDate submitted by the browser. */
    public String getLossDate() {
        return lossDate;
    }

    /** Stores the lossDate submitted by the browser. */
    public void setLossDate(String lossDate) {
        this.lossDate = lossDate;
    }

    /** Returns the description submitted by the browser. */
    public String getDescription() {
        return description;
    }

    /** Stores the description submitted by the browser. */
    public void setDescription(String description) {
        this.description = description;
    }

    /** Clears fields before Struts reuses the form instance. */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        claimantName = null;
        lossDate = null;
        description = null;
    }

    /** Performs the screen-specific validation hook. */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        return super.validate(mapping, request);
    }
}
