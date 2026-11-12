package com.northstar.claims.service;

import java.math.BigDecimal;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.northstar.claims.model.Settlement;

/** Calculates settlement values using the arithmetic rules used by adjusters. */
public class SettlementCalculator {

    /** Settlement totals are kept to the cent. */
    public static final int SCALE = 2;

    public static final int ROUNDING = BigDecimal.ROUND_HALF_EVEN;

    private static SettlementCalculator instance;
    private final Log log = LogFactory.getLog(SettlementCalculator.class);

    private SettlementCalculator() {
    }

    public static synchronized SettlementCalculator getInstance() {
        if (instance == null) {
            instance = new SettlementCalculator();
        }
        return instance;
    }

    /** Applies depreciation, deductible, policy cap, and cent rounding. */
    public Settlement calculate(double coveredAmount, String deductible,
            double depreciation, double policyLimit) {
        double deductibleValue = 0;
        if (deductible != null && deductible.trim().length() > 0) {
            deductibleValue = Double.parseDouble(deductible);
        }
        return calculate(coveredAmount, deductibleValue, depreciation,
                policyLimit);
    }

    /** Applies depreciation, deductible, policy cap, and cent rounding. */
    public Settlement calculate(double coveredAmount, double deductible,
            double depreciation, double policyLimit) {
        BigDecimal gross = BigDecimal.valueOf(coveredAmount)
                .subtract(BigDecimal.valueOf(depreciation));
        BigDecimal afterDeductible = gross.subtract(BigDecimal.valueOf(deductible));
        if (afterDeductible.signum() < 0) {
            afterDeductible = BigDecimal.ZERO;
        }
        BigDecimal limit = BigDecimal.valueOf(policyLimit);
        boolean capped = afterDeductible.compareTo(limit) > 0;
        BigDecimal total = (capped ? limit : afterDeductible)
                .setScale(SCALE, ROUNDING);
        Settlement result = new Settlement();
        result.setCoveredAmount(coveredAmount);
        result.setDeductibleApplied(deductible);
        result.setDepreciation(depreciation);
        result.setCappedAtLimit(capped);
        result.setSettlementAmount(total.doubleValue());
        log.debug("Settlement calculated: " + total);
        return result;
    }

    public double round(double amount) {
        return BigDecimal.valueOf(amount).setScale(SCALE, ROUNDING).doubleValue();
    }
}
