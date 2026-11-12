package com.northstar.claims.boot.settlement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettlementDAO {
    private final JdbcTemplate jdbc;

    public SettlementDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Settlement findById(int id) {
        return first(jdbc.query("select * from SETTLEMENT where settlement_id = ?", this::read, id));
    }

    public List<Settlement> findAll() {
        return jdbc.query("select * from SETTLEMENT order by settlement_id", this::read);
    }

    public int count() { return jdbc.queryForObject("select count(*) from SETTLEMENT", Integer.class); }

    public void delete(int id) { jdbc.update("delete from SETTLEMENT where settlement_id = ?", id); }

    public Settlement findByClaim(int claimId) {
        return first(jdbc.query("select * from SETTLEMENT where claim_id = ? order by settlement_id desc", this::read, claimId));
    }

    public void save(Settlement value) {
        jdbc.update("insert into SETTLEMENT "
                + "(settlement_id,claim_id,covered_amount,deductible_applied,"
                + "depreciation,capped_at_limit,settlement_amount,"
                + "calculated_by,calculated_date) values (?,?,?,?,?,?,?,?,?)",
                value.getSettlementId(), value.getClaimId(), value.getCoveredAmount(),
                value.getDeductibleApplied(), value.getDepreciation(), value.isCappedAtLimit(),
                value.getSettlementAmount(), value.getCalculatedBy(), value.getCalculatedDate());
    }

    private Settlement first(List<Settlement> rows) { return rows.isEmpty() ? null : rows.get(0); }

    private Settlement read(ResultSet rs, int row) throws SQLException {
        Settlement value = new Settlement();
        value.setSettlementId(rs.getInt("settlement_id"));
        value.setClaimId(rs.getInt("claim_id"));
        value.setCoveredAmount(rs.getDouble("covered_amount"));
        value.setDeductibleApplied(rs.getDouble("deductible_applied"));
        value.setDepreciation(rs.getDouble("depreciation"));
        value.setCappedAtLimit(rs.getBoolean("capped_at_limit"));
        value.setSettlementAmount(rs.getDouble("settlement_amount"));
        value.setCalculatedBy(rs.getString("calculated_by"));
        value.setCalculatedDate(rs.getString("calculated_date"));
        return value;
    }
}
