package com.northstar.claims.boot.intake;

public record ClaimNote(int noteId, int claimId, String author, String noteDate, String noteText) {}
