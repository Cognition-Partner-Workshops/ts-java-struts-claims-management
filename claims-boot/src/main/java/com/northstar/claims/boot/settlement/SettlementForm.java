package com.northstar.claims.boot.settlement;

import jakarta.servlet.http.HttpServletRequest;

public class SettlementForm {
    private String coveredAmount;
    private double deductible;
    private String depreciation;
    private String policyLimit;

    public String getCoveredAmount() { return coveredAmount; }
    public void setCoveredAmount(String value) { coveredAmount = value; }
    public double getDeductible() { return deductible; }
    public void setDeductible(double value) { deductible = value; }
    public String getDepreciation() { return depreciation; }
    public void setDepreciation(String value) { depreciation = value; }
    public String getPolicyLimit() { return policyLimit; }
    public void setPolicyLimit(String value) { policyLimit = value; }

    public static SettlementForm bind(HttpServletRequest request) {
        SettlementForm form = new SettlementForm();
        form.setCoveredAmount(request.getParameter("coveredAmount"));
        form.setDepreciation(request.getParameter("depreciation"));
        form.setPolicyLimit(request.getParameter("policyLimit"));
        try { form.setDeductible(Double.parseDouble(request.getParameter("deductible"))); }
        catch (Exception ignored) { form.setDeductible(0); }
        return form;
    }
}
