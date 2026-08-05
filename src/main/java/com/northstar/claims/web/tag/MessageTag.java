package com.northstar.claims.web.tag;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * Buffers an operator message so surrounding pages can render it as one cell.
 */
public class MessageTag extends BodyTagSupport {

    public int doStartTag() throws JspException {
        return EVAL_BODY_BUFFERED;
    }

    public int doEndTag() throws JspException {
        try {
            if (bodyContent != null) {
                String text = bodyContent.getString();
                if (text != null) {
                    pageContext.getOut().print(text.trim());
                }
            }
            return EVAL_PAGE;
        } catch (Exception failure) {
            throw new JspException(failure);
        }
    }

    public void doInitBody() throws JspException {
        if (bodyContent != null) {
            bodyContent.clearBody();
        }
    }

    public void release() {
        if (bodyContent != null) {
            bodyContent.clearBody();
        }
        super.release();
    }
}
