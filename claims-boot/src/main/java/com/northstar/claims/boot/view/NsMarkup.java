package com.northstar.claims.boot.view;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.stereotype.Component;

/** Rendering rules retained from the legacy ns tag library. */
@Component("nsMarkup")
public class NsMarkup {

    public String field(String name, Object value, String type) {
        return "<span id=\"f_" + escape(name) + "\">"
                + escape(format(value, type)) + "</span>";
    }

    public String format(Object value, String type) {
        if (value == null) {
            return "";
        }
        String source = value.toString();
        if ("money".equals(type)) {
            try {
                return String.format("%.2f", Double.parseDouble(source));
            } catch (NumberFormatException notMoney) {
                return source;
            }
        }
        if ("integer".equals(type)) {
            try {
                return String.valueOf((long) Double.parseDouble(source));
            } catch (NumberFormatException notInteger) {
                return source;
            }
        }
        if ("date".equals(type)) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd").parse(source);
                return new SimpleDateFormat("yyyy-MM-dd").format(date);
            } catch (Exception notDate) {
                return source;
            }
        }
        return source;
    }

    public String view(String path) {
        return "<!-- ns:view " + path + " -->";
    }

    public String error(String key) {
        return "<!-- ns:error " + key + " -->";
    }

    private String escape(String source) {
        return source == null ? "" : source.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
