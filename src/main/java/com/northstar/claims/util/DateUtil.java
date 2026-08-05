package com.northstar.claims.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Converts browser dates into the text representation used by the database. */
public class DateUtil {

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

    public static String shortDate(Date value) {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        return format.format(value);
    }

    public static String year(Date value) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy");
        return format.format(value);
    }
}
