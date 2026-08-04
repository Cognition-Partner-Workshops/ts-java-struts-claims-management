package com.northstar.claims.web.form;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * Form bean for the PaymentForm screen.
 * String properties preserve the request-bound behavior of the original UI.
 */
public class PaymentForm extends BaseForm {

    private String payeeName;
    private String amount;
    private String paymentMethod;
    private String checkNumber;

    /** Creates an empty form for the request scope. */
    public PaymentForm() {
        super();
    }

    /** Returns the payeeName submitted by the browser. */
    public String getPayeeName() {
        return payeeName;
    }

    /** Stores the payeeName submitted by the browser. */
    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    /** Returns the amount submitted by the browser. */
    public String getAmount() {
        return amount;
    }

    /** Stores the amount submitted by the browser. */
    public void setAmount(String amount) {
        this.amount = amount;
    }

    /** Returns the paymentMethod submitted by the browser. */
    public String getPaymentMethod() {
        return paymentMethod;
    }

    /** Stores the paymentMethod submitted by the browser. */
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    /** Returns the checkNumber submitted by the browser. */
    public String getCheckNumber() {
        return checkNumber;
    }

    /** Stores the checkNumber submitted by the browser. */
    public void setCheckNumber(String checkNumber) {
        this.checkNumber = checkNumber;
    }

    /** Clears fields before Struts reuses the form instance. */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        payeeName = null;
        amount = null;
        paymentMethod = null;
        checkNumber = null;
    }

    /** Performs the screen-specific validation hook. */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        return super.validate(mapping, request);
    }
}
