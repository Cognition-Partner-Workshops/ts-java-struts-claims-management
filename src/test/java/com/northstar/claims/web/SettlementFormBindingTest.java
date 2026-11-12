package com.northstar.claims.web;

import static org.junit.Assert.assertEquals;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.junit.Test;
import com.northstar.claims.web.form.SettlementForm;

/** Struts populates form beans through BeanUtils with its default converters. */
public class SettlementFormBindingTest {

    @Test
    public void blankDeductibleBindsAsZero() throws Exception {
        assertEquals(0.0, bind("").getDeductible(), 0.0);
    }

    @Test
    public void nonNumericDeductibleBindsAsZero() throws Exception {
        assertEquals(0.0, bind("n/a").getDeductible(), 0.0);
    }

    @Test
    public void numericDeductibleBinds() throws Exception {
        assertEquals(500.0, bind("500.00").getDeductible(), 0.0);
    }

    private SettlementForm bind(String deductible) throws Exception {
        SettlementForm form = new SettlementForm();
        Map values = new HashMap();
        values.put("deductible", new String[] {deductible});
        BeanUtils.populate(form, values);
        return form;
    }
}
