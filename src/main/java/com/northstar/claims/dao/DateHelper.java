package com.northstar.claims.dao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Formats dates for JDBC statements and report headings. */
public class DateHelper {

    private static final String PATTERN = "yyyy-MM-dd";

    public static String format(Date value) {
        if (value == null) {
            return "";
        }
        SimpleDateFormat format = new SimpleDateFormat(PATTERN);
        return format.format(value);
    }

    public static Date parse(String value) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(PATTERN);
        return format.parse(value);
    }

    public static String today() {
        return format(new Date());
    }

    public static String month(Date value) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        return format.format(value);
    }
}
