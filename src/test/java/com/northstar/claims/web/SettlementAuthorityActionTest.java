package com.northstar.claims.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.northstar.claims.dao.SettlementDAO;
import com.northstar.claims.fixture.AdjusterFixtureBuilder;
import com.northstar.claims.model.Settlement;
import com.northstar.claims.util.DatabaseBootstrap;
import com.northstar.claims.web.form.SettlementForm;

/** The adjuster approval limit is enforced by the settlement base action. */
public class SettlementAuthorityActionTest {

    private static final int CLAIM = 118;
    private static String adjuster;

    private Map params;
    private Map requestAttributes;
    private Map sessionAttributes;

    @BeforeClass
    public static void seed() throws Exception {
        System.setProperty("claims.db.path", "target/db/test-northstar");
        DatabaseBootstrap.bootstrap(true);
        adjuster = new AdjusterFixtureBuilder().approvalLimit(750).insert(900);
    }

    @Before
    public void session() {
        params = new HashMap();
        requestAttributes = new HashMap();
        sessionAttributes = new HashMap();
        sessionAttributes.put("user", adjuster);
    }

    @Test
    public void saveAboveLimitIsRefused() throws Exception {
        int before = new SettlementDAO().count();
        ActionForward forward = run(new SettlementSaveAction(), "1900.00", 0);
        assertEquals("refused", forward.getName());
        assertTrue(((List) requestAttributes.get("nsValidationErrors"))
                .contains("errors.settlement.authority"));
        assertEquals(1900.0, ((Double) requestAttributes.get("refusedAmount"))
                .doubleValue(), 0.0);
        assertEquals(before, new SettlementDAO().count());
    }

    @Test
    public void calculateAboveLimitIsRefused() throws Exception {
        ActionForward forward = run(new SettlementCalculateAction(), "2000.00", 100);
        assertEquals("refused", forward.getName());
        assertNull(sessionAttributes.get(SettlementAuthorityAction.PENDING_SETTLEMENT));
    }

    @Test
    public void saveWithinLimitIsStored() throws Exception {
        int before = new SettlementDAO().count();
        ActionForward forward = run(new SettlementSaveAction(), "800.00", 100);
        assertEquals("settlement", forward.getName());
        assertEquals(before + 1, new SettlementDAO().count());
        Settlement saved = new SettlementDAO().findByClaim(CLAIM);
        assertEquals(700.0, saved.getSettlementAmount(), 0.0);
    }

    @Test
    public void screenLogicAloneDoesNotCheckTheLimit() throws Exception {
        params.put("claimId", String.valueOf(CLAIM));
        params.put("coveredAmount", "1900.00");
        SettlementForm form = new SettlementForm();
        int before = new SettlementDAO().count();
        ActionForward forward = new SettlementSaveAction().executeSettlement(
                mapping(), form, request(), null);
        assertEquals("settlement", forward.getName());
        assertEquals(before + 1, new SettlementDAO().count());
    }

    private ActionForward run(SettlementAuthorityAction action, String covered,
            double deductible) throws Exception {
        params.put("claimId", String.valueOf(CLAIM));
        params.put("coveredAmount", covered);
        SettlementForm form = new SettlementForm();
        form.setDeductible(deductible);
        return action.execute(mapping(), form, request(), null);
    }

    private ActionMapping mapping() {
        ActionMapping mapping = new ActionMapping();
        mapping.addForwardConfig(new ActionForward("settlement",
                "/WEB-INF/jsp/settlement/save.jsp", false));
        mapping.addForwardConfig(new ActionForward("refused",
                "/WEB-INF/jsp/settlement/refused.jsp", false));
        return mapping;
    }

    private HttpServletRequest request() {
        final HttpSession session = (HttpSession) proxy(HttpSession.class,
                sessionAttributes, null);
        return (HttpServletRequest) proxy(HttpServletRequest.class,
                requestAttributes, session);
    }

    private Object proxy(Class type, final Map attributes,
            final HttpSession session) {
        return Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] {type}, new InvocationHandler() {
            public Object invoke(Object target, Method method, Object[] args) {
                String name = method.getName();
                if ("getParameter".equals(name)) {
                    return params.get(args[0]);
                }
                if ("getSession".equals(name)) {
                    return session;
                }
                if ("getAttribute".equals(name)) {
                    return attributes.get(args[0]);
                }
                if ("setAttribute".equals(name)) {
                    attributes.put(args[0], args[1]);
                    return null;
                }
                if ("removeAttribute".equals(name)) {
                    attributes.remove(args[0]);
                    return null;
                }
                return null;
            }
        });
    }
}
