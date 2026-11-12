package com.northstar.claims.web;

import static org.junit.Assert.assertEquals;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.junit.Test;
import com.northstar.claims.web.form.IntakeForm;

public class IntakeFormBindingTest {

    @Test
    public void blankInitialReserveBindsAsZero() throws Exception {
        IntakeForm form = new IntakeForm();
        Map values = new HashMap();
        values.put("initialReserve", new String[] {""});
        values.put("claimantName", new String[] {"Blank Reserve Claimant"});
        BeanUtils.populate(form, values);
        assertEquals(0.0, form.getInitialReserve(), 0.0);
    }
}
