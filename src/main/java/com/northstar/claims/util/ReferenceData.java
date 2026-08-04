package com.northstar.claims.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Holds the small reference lists displayed by claims administration pages. */
public class ReferenceData {

    private static final Log log = LogFactory.getLog(ReferenceData.class);
    private final Map descriptions = new Hashtable();
    private final Vector lines = new Vector();
    private final List statuses = new ArrayList();

    public ReferenceData() {
        loadDefaults();
    }

    private void loadDefaults() {
        addLine("AUTO", "Automobile");
        addLine("HOMEOWNERS", "Homeowners");
        addLine("COMMERCIAL_PROPERTY", "Commercial Property");
        addLine("GENERAL_LIABILITY", "General Liability");
        statuses.add("OPEN");
        statuses.add("INVESTIGATING");
        statuses.add("APPROVED");
        statuses.add("DENIED");
        statuses.add("CLOSED");
    }

    public void addLine(String code, String description) {
        lines.add(code);
        descriptions.put(code, description);
    }

    public List lines() {
        return new ArrayList(lines);
    }

    public List statuses() {
        return new ArrayList(statuses);
    }

    public String description(String code) {
        Object value = descriptions.get(code);
        return value == null ? "" : String.valueOf(value);
    }

    public boolean isLine(String code) {
        return lines.contains(code);
    }

    public boolean isStatus(String status) {
        return statuses.contains(status);
    }

    public int lineCount() {
        return lines.size();
    }

    public int statusCount() {
        return statuses.size();
    }

    public String lineLabel(String code) {
        StringBuffer label = new StringBuffer();
        label.append(code);
        label.append(" - ");
        label.append(description(code));
        return label.toString();
    }

    public String summary() {
        String text = "lines=" + lineCount() + ", statuses=" + statusCount();
        log.debug(text);
        System.out.println(text);
        return text;
    }
}
