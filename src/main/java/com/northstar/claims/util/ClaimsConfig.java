package com.northstar.claims.util;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Loads fixed operating values used by the claims application.
 */
public final class ClaimsConfig {

    private static final Log log = LogFactory.getLog(ClaimsConfig.class);

    private static final Properties properties = new Properties();

    private static final Hashtable cache = new Hashtable();

    static {
        loadProperties();
    }

    private ClaimsConfig() {
    }

    /** Returns a configured value and remembers it in the process cache. */
    public static String get(String key) {
        Object cached = cache.get(key);
        if (cached != null) {
            return String.valueOf(cached);
        }
        String value = properties.getProperty(key);
        if (value == null) {
            value = "";
        }
        cache.put(key, value);
        return value;
    }

    /** Reads the application properties shipped with the web archive. */
    private static void loadProperties() {
        InputStream stream = null;
        try {
            stream = ClaimsConfig.class.getClassLoader()
                    .getResourceAsStream("northstar.properties");
            if (stream != null) {
                properties.load(stream);
            }
        } catch (Exception failure) {
            log.error("Unable to load NorthStar configuration", failure);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
