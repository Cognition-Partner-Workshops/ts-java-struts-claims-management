package com.northstar.claims.web.form;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * Form bean for the SettlementForm screen.
 * String properties preserve the request-bound behavior of the original UI.
 */
public class SettlementForm extends BaseForm {

    private String coveredAmount;
    private String deductible;
    private String depreciation;
    private String policyLimit;

    /** Creates an empty form for the request scope. */
    public SettlementForm() {
        super();
    }

    /** Returns the coveredAmount submitted by the browser. */
    public String getCoveredAmount() {
        return coveredAmount;
    }

    /** Stores the coveredAmount submitted by the browser. */
    public void setCoveredAmount(String coveredAmount) {
        this.coveredAmount = coveredAmount;
    }

    /** Returns the deductible submitted by the browser. */
    public String getDeductible() {
        return deductible;
    }

    /** Stores the deductible submitted by the browser. */
    public void setDeductible(String deductible) {
        this.deductible = deductible;
    }

    /** Returns the depreciation submitted by the browser. */
    public String getDepreciation() {
        return depreciation;
    }

    /** Stores the depreciation submitted by the browser. */
    public void setDepreciation(String depreciation) {
        this.depreciation = depreciation;
    }

    /** Returns the policyLimit submitted by the browser. */
    public String getPolicyLimit() {
        return policyLimit;
    }

    /** Stores the policyLimit submitted by the browser. */
    public void setPolicyLimit(String policyLimit) {
        this.policyLimit = policyLimit;
    }

    /** Clears fields before Struts reuses the form instance. */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        coveredAmount = null;
        deductible = null;
        depreciation = null;
        policyLimit = null;
    }

    /** Performs the screen-specific validation hook. */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        return super.validate(mapping, request);
    }
}
