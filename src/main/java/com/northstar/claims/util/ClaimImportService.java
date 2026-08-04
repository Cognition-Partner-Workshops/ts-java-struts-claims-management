package com.northstar.claims.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Converts delimited intake records into validated claim value maps. */
public class ClaimImportService {

    private static final Log log = LogFactory.getLog(ClaimImportService.class);
    private final ClaimsValidator validator = new ClaimsValidator();
    private final Vector accepted = new Vector();
    private final Vector rejected = new Vector();

    public void importLine(String line) {
        validator.clear();
        Map values = parse(line);
        validator.required("claimant", (String) values.get("claimant"));
        validator.required("lossDate", (String) values.get("lossDate"));
        validator.required("description", (String) values.get("description"));
        if (validator.valid()) {
            accepted.add(values);
        } else {
            rejected.add(line);
        }
    }

    public Map parse(String line) {
        Map values = new Hashtable();
        if (line == null) {
            return values;
        }
        String[] parts = line.split("\\|", -1);
        if (parts.length > 0) {
            values.put("claimant", parts[0]);
        }
        if (parts.length > 1) {
            values.put("lossDate", parts[1]);
        }
        if (parts.length > 2) {
            values.put("description", parts[2]);
        }
        if (parts.length > 3) {
            values.put("lossType", parts[3]);
        }
        return values;
    }

    public List accepted() {
        return new ArrayList(accepted);
    }

    public List rejected() {
        return new ArrayList(rejected);
    }

    public int acceptedCount() {
        return accepted.size();
    }

    public int rejectedCount() {
        return rejected.size();
    }

    public boolean hasErrors() {
        return rejectedCount() > 0;
    }

    public void clear() {
        accepted.clear();
        rejected.clear();
    }

    public String report() {
        StringBuffer text = new StringBuffer();
        text.append("accepted=");
        text.append(acceptedCount());
        text.append(", rejected=");
        text.append(rejectedCount());
        log.info(text.toString());
        System.out.println(text.toString());
        return text.toString();
    }
}
