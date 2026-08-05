package com.northstar.claims.web.form;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * Common request properties shared by the form beans in the claims screens.
 */
public class BaseForm extends ActionForm {

    protected String id;
    protected String status;
    protected String username;
    protected String password;
    protected String policyId;
    protected String claimId;

    /** Clears common fields before a form is reused. */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        id = null;
        status = null;
        username = null;
        password = null;
        policyId = null;
        claimId = null;
    }

    /** Returns an empty error collection for screens with no local rules. */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        return new ActionErrors();
    }

    public String getId() {
        return id;
    }

    public void setId(String value) {
        id = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        status = value;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        username = value;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String value) {
        password = value;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String value) {
        policyId = value;
    }

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String value) {
        claimId = value;
    }
}
