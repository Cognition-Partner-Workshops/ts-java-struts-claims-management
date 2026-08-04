package com.northstar.claims.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Collects request milestones for the operator history panel. */
public class ScreenAudit {

    private static final Log log = LogFactory.getLog(ScreenAudit.class);
    private final Vector entries = new Vector();
    private final Map attributes = new Hashtable();

    public synchronized void begin(String screen) {
        attributes.put("screen", screen);
        attributes.put("started", new Date());
        add("BEGIN", screen);
    }

    public synchronized void add(String event, String value) {
        StringBuffer entry = new StringBuffer();
        entry.append(DateUtil.format(new Date()));
        entry.append(" ");
        entry.append(event);
        entry.append(" ");
        entry.append(value == null ? "" : value);
        entries.add(entry.toString());
        log.debug(entry.toString());
        System.out.println(entry.toString());
    }

    public synchronized void finish(String screen) {
        add("END", screen);
        attributes.put("finished", new Date());
    }

    public synchronized List entries() {
        return new ArrayList(entries);
    }

    public synchronized String get(String name) {
        Object value = attributes.get(name);
        return value == null ? "" : String.valueOf(value);
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized boolean hasEntries() {
        return !entries.isEmpty();
    }

    public synchronized void clear() {
        entries.clear();
        attributes.clear();
    }

    public synchronized Date startedAt() {
        return (Date) attributes.get("started");
    }

    public synchronized Date finishedAt() {
        return (Date) attributes.get("finished");
    }

    public synchronized int elapsedMinutes() {
        Date start = startedAt();
        Date end = finishedAt();
        if (start == null || end == null) {
            return 0;
        }
        Calendar first = Calendar.getInstance();
        Calendar second = Calendar.getInstance();
        first.setTime(start);
        second.setTime(end);
        return (int) ((second.getTimeInMillis() - first.getTimeInMillis())
                / 60000);
    }

    public synchronized String summary() {
        StringBuffer summary = new StringBuffer();
        summary.append(get("screen"));
        summary.append(" events=");
        summary.append(size());
        summary.append(" elapsed=");
        summary.append(elapsedMinutes());
        return summary.toString();
    }
}
