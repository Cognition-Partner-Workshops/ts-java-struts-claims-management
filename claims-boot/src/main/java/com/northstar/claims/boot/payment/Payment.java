package com.northstar.claims.boot.payment;

/** Request-local legacy PAYMENT row. */
public class Payment {
    private int paymentId;
    private int claimId;
    private int settlementId;
    private String payeeName;
    private double amount;
    private String paymentMethod;
    private String checkNumber;
    private String issuedDate;
    private String status;

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int value) { paymentId = value; }

    public int getClaimId() { return claimId; }
    public void setClaimId(int value) { claimId = value; }

    public int getSettlementId() { return settlementId; }
    public void setSettlementId(int value) { settlementId = value; }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String value) { payeeName = value; }

    public double getAmount() { return amount; }
    public void setAmount(double value) { amount = value; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String value) { paymentMethod = value; }

    public String getCheckNumber() { return checkNumber; }
    public void setCheckNumber(String value) { checkNumber = value; }

    public String getIssuedDate() { return issuedDate; }
    public void setIssuedDate(String value) { issuedDate = value; }

    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
}
