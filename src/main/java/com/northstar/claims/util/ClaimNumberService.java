package com.northstar.claims.util;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Creates and checks the claim numbers shown on correspondence. */
public class ClaimNumberService {

    private static final Log log = LogFactory.getLog(ClaimNumberService.class);
    private final List issued = new ArrayList();

    public String create(int id) {
        String number = "CLM-" + Utils.pad(id, 5);
        issued.add(number);
        return number;
    }

    public boolean isValid(String number) {
        return number != null && number.matches("CLM-[0-9]{5}");
    }

    public int numeric(String number) {
        if (!isValid(number)) {
            return 0;
        }
        return Utils.toInt(number.substring(4));
    }

    public void record(String number) {
        if (isValid(number) && !issued.contains(number)) {
            issued.add(number);
        }
    }

    public List issued() {
        return new ArrayList(issued);
    }

    public int count() {
        return issued.size();
    }

    public String first() {
        return issued.isEmpty() ? "" : String.valueOf(issued.get(0));
    }

    public String last() {
        return issued.isEmpty() ? "" : String.valueOf(issued.get(issued.size() - 1));
    }

    public String summary() {
        String text = "claim numbers issued=" + count();
        log.info(text);
        System.out.println(text);
        return text;
    }
}
