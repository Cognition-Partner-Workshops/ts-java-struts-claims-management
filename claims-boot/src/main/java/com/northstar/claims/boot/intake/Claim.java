package com.northstar.claims.boot.intake;

/** The CLAIM columns displayed by intake and workbench screens. */
public record Claim(int claimId, String claimNumber, int policyId, String claimantName,
                    String lossDate, String reportedDate, String lossType, String description,
                    String status, double reserveAmount, String assignedAdjuster,
                    String createdBy, String createdDate) {}
