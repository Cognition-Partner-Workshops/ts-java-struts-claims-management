package com.northstar.claims.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Stores operator messages before they are written to the session. */
public class OperatorMessage {

    private static final Log log = LogFactory.getLog(OperatorMessage.class);
    private final Map values = new Hashtable();
    private final List history = new ArrayList();

    public void put(String key, String message) {
        values.put(key, message);
        history.add(key + "=" + message);
    }

    public String get(String key) {
        Object value = values.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }

    public List keys() {
        return new ArrayList(values.keySet());
    }

    public List history() {
        return new ArrayList(history);
    }

    public void remove(String key) {
        values.remove(key);
    }

    public void clear() {
        values.clear();
        history.clear();
    }

    public int size() {
        return values.size();
    }

    public String summary() {
        StringBuffer text = new StringBuffer();
        text.append("messages=");
        text.append(size());
        log.debug(text.toString());
        System.out.println(text.toString());
        return text.toString();
    }
}
