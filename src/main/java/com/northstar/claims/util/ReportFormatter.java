package com.northstar.claims.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Formats raw aggregate maps for printed management reports. */
public class ReportFormatter {

    private static final Log log = LogFactory.getLog(ReportFormatter.class);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public String money(Object value) {
        if (value == null) {
            return "0.00";
        }
        return Utils.formatMoney(Utils.toDouble(String.valueOf(value)));
    }

    public String percent(Object value) {
        if (value == null) {
            return "0.00%";
        }
        return Utils.formatMoney(Utils.toDouble(String.valueOf(value)) * 100) + "%";
    }

    public String date(Date value) {
        return value == null ? "" : dateFormat.format(value);
    }

    public String title(String reportName) {
        StringBuffer title = new StringBuffer();
        title.append("NorthStar Claims - ");
        title.append(reportName == null ? "Report" : reportName);
        return title.toString();
    }

    public List copyRows(List rows) {
        List copy = new ArrayList();
        if (rows != null) {
            for (int i = 0; i < rows.size(); i++) {
                Object row = rows.get(i);
                if (row instanceof Map) {
                    copy.add(row);
                }
            }
        }
        return copy;
    }

    public String rowLabel(Map row, String key) {
        Object value = row == null ? null : row.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    public double rowAmount(Map row, String key) {
        return Utils.toDouble(rowLabel(row, key));
    }

    public boolean isEmpty(Map row, String key) {
        return Utils.isBlank(rowLabel(row, key));
    }

    public String runDate() {
        Calendar calendar = Calendar.getInstance();
        return date(calendar.getTime());
    }

    public String footer(String operator) {
        String text = "Prepared by " + Utils.defaultValue(operator, "claims operations");
        log.debug(text);
        System.out.println(text);
        return text;
    }
}
