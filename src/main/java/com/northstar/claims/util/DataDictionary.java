package com.northstar.claims.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Describes database fields used by import, audit, and report support tools. */
public class DataDictionary {

    private static final Log log = LogFactory.getLog(DataDictionary.class);
    private final Map fields = new Hashtable();

    public DataDictionary() {
        add("claim_id", "Claim identifier");
        add("claim_number", "Claim number");
        add("policy_id", "Policy identifier");
        add("reserve_amount", "Current reserve");
        add("settlement_amount", "Calculated settlement");
        add("assigned_adjuster", "Assigned adjuster");
    }

    public void add(String name, String description) {
        fields.put(name, description);
    }

    public String description(String name) {
        Object value = fields.get(name);
        return value == null ? "" : String.valueOf(value);
    }

    public boolean contains(String name) {
        return fields.containsKey(name);
    }

    public List names() {
        return new ArrayList(fields.keySet());
    }

    public int size() {
        return fields.size();
    }

    public String describeAll() {
        StringBuffer result = new StringBuffer();
        List names = names();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                result.append(';');
            }
            String name = String.valueOf(names.get(i));
            result.append(name);
            result.append('=');
            result.append(description(name));
        }
        log.debug(result.toString());
        System.out.println(result.toString());
        return result.toString();
    }
}
