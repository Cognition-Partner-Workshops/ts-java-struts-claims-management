package com.northstar.claims.boot.payment;

/** Request-local legacy string form; validation is intentionally disabled. */
public class PaymentForm {
    private String id;
    private String status;
    private String username;
    private String password;
    private String policyId;
    private String claimId;
    private String payeeName;
    private String amount;
    private String paymentMethod;
    private String checkNumber;

    public String getId() { return id; }
    public void setId(String value) { id = value; }

    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }

    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String value) { policyId = value; }

    public String getClaimId() { return claimId; }
    public void setClaimId(String value) { claimId = value; }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String value) { payeeName = value; }

    public String getAmount() { return amount; }
    public void setAmount(String value) { amount = value; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String value) { paymentMethod = value; }

    public String getCheckNumber() { return checkNumber; }
    public void setCheckNumber(String value) { checkNumber = value; }
}
