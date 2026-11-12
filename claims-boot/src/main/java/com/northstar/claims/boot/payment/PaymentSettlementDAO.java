package com.northstar.claims.boot.payment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Payment-owned projection of SettlementDAO.findByClaim; keeps the legacy query. */
@Repository
public class PaymentSettlementDAO {
    public record Settlement(int settlementId, double settlementAmount) {}

    private final JdbcTemplate jdbc;

    public PaymentSettlementDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Settlement findByClaim(int claimId) {
        var rows = jdbc.query("select * from SETTLEMENT where claim_id = ? order by settlement_id desc",
                (rs, row) -> new Settlement(rs.getInt("settlement_id"), rs.getDouble("settlement_amount")),
                claimId);
        return rows.isEmpty() ? null : rows.getFirst();
    }
}
