package com.northstar.claims.boot.policy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PolicyDAO {
    private final JdbcTemplate jdbc;

    public PolicyDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Policy findById(int id) {
        List<Policy> rows = jdbc.query("select * from POLICY where policy_id = ?", this::read, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public List<Policy> findAll() {
        return jdbc.query("select * from POLICY order by policy_id", this::read);
    }

    public int count() {
        return jdbc.queryForObject("select count(*) from POLICY", Integer.class);
    }

    public void delete(int id) {
        jdbc.update("delete from POLICY where policy_id = ?", id);
    }

    public List<Policy> findByLine(String lineOfBusiness) {
        // Preserve the legacy concatenated filter, including its failure behavior.
        StringBuilder sql = new StringBuilder();
        sql.append("select * from POLICY where line_of_business = '");
        sql.append(lineOfBusiness);
        sql.append("' order by policy_number");
        return jdbc.query(sql.toString(), this::read);
    }

    public List<Policy> search(String term) {
        return jdbc.query("select * from POLICY where policy_number like ? or insured_name like ?",
                this::read, "%" + term + "%", "%" + term + "%");
    }

    public void updateStatus(int id, String status) {
        jdbc.update("update POLICY set status = ? where policy_id = ?", status, id);
    }

    private Policy read(ResultSet rs, int rowNumber) throws SQLException {
        Policy value = new Policy();
        value.setPolicyId(rs.getInt("policy_id"));
        value.setPolicyNumber(rs.getString("policy_number"));
        value.setLineOfBusiness(rs.getString("line_of_business"));
        value.setInsuredName(rs.getString("insured_name"));
        value.setInsuredAddress(rs.getString("insured_address"));
        value.setEffectiveDate(rs.getString("effective_date"));
        value.setExpiryDate(rs.getString("expiry_date"));
        value.setPolicyLimit(rs.getDouble("policy_limit"));
        value.setDeductible(rs.getDouble("deductible"));
        value.setAnnualPremium(rs.getDouble("annual_premium"));
        value.setStatus(rs.getString("status"));
        return value;
    }
}
