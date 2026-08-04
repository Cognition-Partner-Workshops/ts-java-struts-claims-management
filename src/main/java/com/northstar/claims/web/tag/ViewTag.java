package com.northstar.claims.web.tag;

import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Emits the stable view marker used by transcript capture.
 */
public class ViewTag extends TagSupport {

    private String path;

    public void setPath(String value) {
        path = value;
    }

    public int doStartTag() throws JspException {
        try {
            pageContext.getOut().write("<!-- ns:view " + path + " -->");
        } catch (IOException failure) {
            throw new JspException(failure);
        }
        return SKIP_BODY;
    }

    public int doEndTag() throws JspException {
        path = null;
        return EVAL_PAGE;
    }
}
