package com.northstar.claims.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Performs the field checks shared by import jobs and Struts form beans. */
public class ClaimsValidator {

    private static final Log log = LogFactory.getLog(ClaimsValidator.class);
    private final List messages = new ArrayList();

    public boolean required(String name, String value) {
        if (Utils.isBlank(value)) {
            messages.add(name + " is required");
            return false;
        }
        return true;
    }

    public boolean number(String name, String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (Exception failure) {
            messages.add(name + " must be numeric");
            return false;
        }
    }

    public boolean integer(String name, String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (Exception failure) {
            messages.add(name + " must be an integer");
            return false;
        }
    }

    public boolean date(String name, String value) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            format.parse(value);
            return true;
        } catch (ParseException failure) {
            messages.add(name + " must be a date");
            return false;
        }
    }

    public boolean length(String name, String value, int maximum) {
        if (value != null && value.length() > maximum) {
            messages.add(name + " is too long");
            return false;
        }
        return true;
    }

    public List messages() {
        return new ArrayList(messages);
    }

    public boolean valid() {
        return messages.isEmpty();
    }

    public void clear() {
        messages.clear();
    }

    public String firstMessage() {
        return messages.isEmpty() ? "" : String.valueOf(messages.get(0));
    }

    public int ageInDays(String reportedDate, String asOf) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Calendar first = Calendar.getInstance();
            Calendar second = Calendar.getInstance();
            first.setTime(format.parse(reportedDate));
            second.setTime(format.parse(asOf));
            return (int) ((second.getTimeInMillis() - first.getTimeInMillis()) / 86400000);
        } catch (Exception failure) {
            log.warn("Unable to calculate claim age", failure);
            return 0;
        }
    }

    public String summary() {
        String text = "valid=" + valid() + ", errors=" + messages.size();
        log.debug(text);
        System.out.println(text);
        return text;
    }
}
