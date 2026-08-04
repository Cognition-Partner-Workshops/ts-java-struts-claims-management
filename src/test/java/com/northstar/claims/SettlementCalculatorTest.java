package com.northstar.claims;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.northstar.claims.model.Settlement;
import com.northstar.claims.service.SettlementCalculator;

/** Verifies the arithmetic used by the settlement workbench. */
public class SettlementCalculatorTest {

    private final SettlementCalculator calculator =
            SettlementCalculator.getInstance();

    @Test
    public void normalCase() {
        Settlement result = calculator.calculate(10000, "500", 100, 20000);
        assertEquals(9400.0, result.getSettlementAmount(), 0.0);
        assertEquals(500.0, result.getDeductibleApplied(), 0.0);
    }

    @Test
    public void blankDeductible() {
        Settlement result = calculator.calculate(10000, "", 100, 20000);
        assertEquals(9900.0, result.getSettlementAmount(), 0.0);
    }

    @Test
    public void deductibleExceedsLoss() {
        Settlement result = calculator.calculate(1000, "2000", 100, 20000);
        assertEquals(0.0, result.getSettlementAmount(), 0.0);
    }

    @Test
    public void policyLimitCaps() {
        Settlement result = calculator.calculate(20000, "500", 100, 10000);
        assertEquals(10000.0, result.getSettlementAmount(), 0.0);
        assertTrue(result.isCappedAtLimit());
    }

    @Test
    public void halfCentUsesLegacyDoubleMath() {
        Settlement result = calculator.calculate(1.005, "0", 0, 100);
        assertEquals(1.0, result.getSettlementAmount(), 0.0);
    }

    @Test
    public void zeroLimitDoesNotProduceNegativeAmount() {
        Settlement result = calculator.calculate(100, "0", 0, 0);
        assertEquals(0.0, result.getSettlementAmount(), 0.0);
    }
}
