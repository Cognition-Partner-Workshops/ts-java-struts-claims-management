package com.northstar.claims.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Produces the nightly work files consumed by the claims operations desk. */
public class LegacyBatchJob {

    private static final Log log = LogFactory.getLog(LegacyBatchJob.class);
    private final List workItems = new ArrayList();

    public void read(File source) throws Exception {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(source));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() > 0) {
                    workItems.add(line.trim());
                }
            }
        } finally {
            try { reader.close(); } catch (Exception e) {}
        }
    }

    public void add(String claimNumber) {
        if (claimNumber != null && claimNumber.length() > 0) {
            workItems.add(claimNumber);
        }
    }

    public int size() {
        return workItems.size();
    }

    public String first() {
        return workItems.isEmpty() ? "" : String.valueOf(workItems.get(0));
    }

    public String last() {
        return workItems.isEmpty() ? "" : String.valueOf(workItems.get(workItems.size() - 1));
    }

    public List items() {
        return new ArrayList(workItems);
    }

    public void sort() {
        java.util.Collections.sort(workItems);
    }

    public void remove(String claimNumber) {
        workItems.remove(claimNumber);
    }

    public boolean contains(String claimNumber) {
        return workItems.contains(claimNumber);
    }

    public String buildHeader(String office) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        StringBuffer header = new StringBuffer();
        header.append(office);
        header.append('|');
        header.append(format.format(new Date()));
        header.append('|');
        header.append(size());
        return header.toString();
    }

    public void write(File target, String office) throws Exception {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(target));
            writer.println(buildHeader(office));
            for (int i = 0; i < workItems.size(); i++) {
                writer.println(workItems.get(i));
            }
        } finally {
            try { writer.close(); } catch (Exception e) {}
        }
    }

    public Date nextRun(Date current) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        calendar.add(Calendar.DATE, 1);
        return calendar.getTime();
    }

    public String status() {
        String text = "BATCH ITEMS=" + size();
        log.info(text);
        System.out.println(text);
        return text;
    }
}
