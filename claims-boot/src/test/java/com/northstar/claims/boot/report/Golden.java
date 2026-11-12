package com.northstar.claims.boot.report;

import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Writes report snapshots as canonical JSON (sorted keys, LF line endings,
 * locale-free numbers) and compares them with the checked-in goldens.
 * Copy of the legacy characterization Golden: goldens are only compared, never written.
 */
public final class Golden {

    /** The legacy goldens from the root project, read in place and never rewritten from here. */
    public static final String ROOT = System.getProperty("characterization.root",
            "../src/test/resources/characterization/reports");

    private Golden() {
    }

    public static void verify(String relativePath, Object snapshot) throws Exception {
        String actual = toJson(snapshot, 0) + "\n";
        File golden = new File(ROOT, relativePath);
        if (!golden.isFile()) {
            write(new File("target/characterization-actual", relativePath), actual);
            fail("Missing golden " + golden);
        }
        String expected = read(golden).replace("\r\n", "\n");
        if (!expected.equals(actual)) {
            File out = new File("target/characterization-actual", relativePath);
            write(out, actual);
            fail("Report output differs from golden " + golden + " (actual written to "
                    + out + ")\n" + firstDifference(expected, actual));
        }
    }

    /** Sums every numeric column of the rows (ratios excepted), keyed by column name. */
    public static Map totals(List rows) {
        Map sums = new TreeMap();
        for (Iterator i = rows.iterator(); i.hasNext();) {
            Map row = (Map) i.next();
            for (Iterator k = row.entrySet().iterator(); k.hasNext();) {
                Map.Entry e = (Map.Entry) k.next();
                if (e.getValue() instanceof Number
                        && !String.valueOf(e.getKey()).endsWith("Ratio")) {
                    BigDecimal sum = (BigDecimal) sums.get(e.getKey());
                    BigDecimal value = decimal((Number) e.getValue());
                    sums.put(e.getKey(), sum == null ? value : sum.add(value));
                }
            }
        }
        Map totals = new TreeMap();
        totals.put("rowCount", Integer.valueOf(rows.size()));
        if (!sums.isEmpty()) {
            totals.put("sum", sums);
        }
        return totals;
    }

    static BigDecimal decimal(Number n) {
        if (n instanceof BigDecimal) {
            return (BigDecimal) n;
        }
        if (n instanceof Double || n instanceof Float) {
            return new BigDecimal(Double.toString(n.doubleValue()));
        }
        return BigDecimal.valueOf(n.longValue());
    }

    static String toJson(Object value, int indent) {
        StringBuilder out = new StringBuilder();
        append(out, value, indent);
        return out.toString();
    }

    private static void append(StringBuilder out, Object value, int indent) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof Map) {
            Map sorted = new TreeMap((Map) value);
            if (sorted.isEmpty()) {
                out.append("{}");
                return;
            }
            out.append("{\n");
            for (Iterator i = sorted.entrySet().iterator(); i.hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                pad(out, indent + 1);
                string(out, String.valueOf(e.getKey()));
                out.append(": ");
                append(out, e.getValue(), indent + 1);
                out.append(i.hasNext() ? ",\n" : "\n");
            }
            pad(out, indent);
            out.append("}");
        } else if (value instanceof List) {
            List list = (List) value;
            if (list.isEmpty()) {
                out.append("[]");
                return;
            }
            out.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                pad(out, indent + 1);
                append(out, list.get(i), indent + 1);
                out.append(i + 1 < list.size() ? ",\n" : "\n");
            }
            pad(out, indent);
            out.append("]");
        } else if (value instanceof Number) {
            out.append(decimal((Number) value).toPlainString());
        } else if (value instanceof Boolean) {
            out.append(value.toString());
        } else {
            string(out, value.toString());
        }
    }

    private static void pad(StringBuilder out, int indent) {
        for (int i = 0; i < indent; i++) {
            out.append("  ");
        }
    }

    private static void string(StringBuilder out, String s) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\t') {
                out.append("\\t");
            } else if (c < 0x20 || c > 0x7e) {
                out.append(String.format(java.util.Locale.ROOT, "\\u%04x", Integer.valueOf(c)));
            } else {
                out.append(c);
            }
        }
        out.append('"');
    }

    private static String firstDifference(String expected, String actual) {
        String[] e = expected.split("\n", -1);
        String[] a = actual.split("\n", -1);
        for (int i = 0; i < Math.max(e.length, a.length); i++) {
            String el = i < e.length ? e[i] : "<eof>";
            String al = i < a.length ? a[i] : "<eof>";
            if (!el.equals(al)) {
                return "line " + (i + 1) + "\n  expected: " + el + "\n  actual:   " + al;
            }
        }
        return "";
    }

    static String read(File file) throws Exception {
        InputStream in = new FileInputStream(file);
        try {
            return readFully(in);
        } finally {
            in.close();
        }
    }

    static String readFully(InputStream in) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0) {
            bytes.write(buffer, 0, n);
        }
        return new String(bytes.toByteArray(), "UTF-8");
    }

    private static void write(File file, String text) throws Exception {
        file.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(file);
        try {
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    static List list() {
        return new ArrayList();
    }
}
