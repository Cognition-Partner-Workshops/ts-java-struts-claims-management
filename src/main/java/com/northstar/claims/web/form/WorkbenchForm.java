package com.northstar.claims.web.form;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * Form bean for the WorkbenchForm screen.
 * String properties preserve the request-bound behavior of the original UI.
 */
public class WorkbenchForm extends BaseForm {

    private String assignedAdjuster;
    private String reserveAmount;
    private String noteText;

    /** Creates an empty form for the request scope. */
    public WorkbenchForm() {
        super();
    }

    /** Returns the assignedAdjuster submitted by the browser. */
    public String getAssignedAdjuster() {
        return assignedAdjuster;
    }

    /** Stores the assignedAdjuster submitted by the browser. */
    public void setAssignedAdjuster(String assignedAdjuster) {
        this.assignedAdjuster = assignedAdjuster;
    }

    /** Returns the reserveAmount submitted by the browser. */
    public String getReserveAmount() {
        return reserveAmount;
    }

    /** Stores the reserveAmount submitted by the browser. */
    public void setReserveAmount(String reserveAmount) {
        this.reserveAmount = reserveAmount;
    }

    /** Returns the noteText submitted by the browser. */
    public String getNoteText() {
        return noteText;
    }

    /** Stores the noteText submitted by the browser. */
    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    /** Clears fields before Struts reuses the form instance. */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        assignedAdjuster = null;
        reserveAmount = null;
        noteText = null;
    }

    /** Performs the screen-specific validation hook. */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        return super.validate(mapping, request);
    }
}
