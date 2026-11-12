package com.northstar.claims;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import org.junit.Test;
import com.northstar.claims.characterization.SettlementCharacterizationTest;
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
    public void halfCentRoundsToEvenCent() {
        assertEquals(1.0, calculator.calculate(1.005, "0", 0, 100)
                .getSettlementAmount(), 0.0);
        assertEquals(1.02, calculator.calculate(1.015, "0", 0, 100)
                .getSettlementAmount(), 0.0);
        assertEquals(1.02, calculator.calculate(1.025, "0", 0, 100)
                .getSettlementAmount(), 0.0);
    }

    @Test
    public void zeroLimitDoesNotProduceNegativeAmount() {
        Settlement result = calculator.calculate(100, "0", 0, 0);
        assertEquals(0.0, result.getSettlementAmount(), 0.0);
    }

    @Test
    public void nullDeductibleAppliesZero() {
        Settlement result = calculator.calculate(10000, (String) null, 100, 20000);
        assertEquals(9900.0, result.getSettlementAmount(), 0.0);
        assertEquals(0.0, result.getDeductibleApplied(), 0.0);
    }

    @Test
    public void depreciationIsSubtractedWithTheDeductible() {
        Settlement result = calculator.calculate(5000, "500", 1000.50, 25000);
        assertEquals(3499.50, result.getSettlementAmount(), 0.0);
        assertEquals(1000.50, result.getDepreciation(), 0.0);
    }

    @Test
    public void netLossAtTheLimitIsNotCapped() {
        Settlement atLimit = calculator.calculate(10500, "500", 0, 10000);
        assertEquals(10000.0, atLimit.getSettlementAmount(), 0.0);
        assertFalse(atLimit.isCappedAtLimit());
        Settlement centOver = calculator.calculate(10500.01, "500", 0, 10000);
        assertEquals(10000.0, centOver.getSettlementAmount(), 0.0);
        assertTrue(centOver.isCappedAtLimit());
    }

    /** SD-17: the full deductible is recorded even when only part of it was absorbed. */
    @Test
    public void deductibleAppliedRecordsTheFullDeductible() {
        Settlement result = calculator.calculate(1000, "2000", 100, 20000);
        assertEquals(2000.0, result.getDeductibleApplied(), 0.0);
    }

    /** SD-05: the cap flag compares the unrounded amount. */
    @Test
    public void capFlagIsSetBeforeRounding() {
        Settlement result = calculator.calculate(1000.004, "0", 0, 1000);
        assertEquals(1000.0, result.getSettlementAmount(), 0.0);
        assertTrue(result.isCappedAtLimit());
    }

    /** SD-02, SD-03 and SD-04: negative inputs are not rejected. */
    @Test
    public void negativeInputsAreNotRejected() {
        assertEquals(-50.0, calculator.calculate(100, "0", 0, -50)
                .getSettlementAmount(), 0.0);
        assertEquals(1500.0, calculator.calculate(1000, "-500", 0, 10000)
                .getSettlementAmount(), 0.0);
        assertEquals(1200.0, calculator.calculate(1000, "0", -200, 10000)
                .getSettlementAmount(), 0.0);
        assertEquals(0.0, calculator.calculate(-100, "0", 0, 1000)
                .getSettlementAmount(), 0.0);
    }

    @Test(expected = NumberFormatException.class)
    public void malformedDeductibleIsRejected() {
        calculator.calculate(1000, "1,000", 0, 10000);
    }

    @Test
    public void everySuspectedDefectInTheTableIsDocumented() throws Exception {
        String defects = new String(Files.readAllBytes(
                new File("docs/migration/SUSPECTED-DEFECTS.md").toPath()),
                StandardCharsets.UTF_8);
        for (Map<String, String> row : SettlementCharacterizationTest.readTable()) {
            String defect = row.get("suspected_defect");
            if (defect.length() > 0) {
                assertTrue(row.get("case_id") + " cites " + defect,
                        defects.contains("## " + defect + " "));
                assertTrue(defect + " lists " + row.get("case_id"),
                        defects.contains("`" + row.get("case_id") + "`"));
            }
        }
    }
}
