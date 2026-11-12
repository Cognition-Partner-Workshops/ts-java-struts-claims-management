package com.northstar.claims.boot.payment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Legacy PAYMENT SQL, with connection management delegated to JdbcTemplate. */
@Repository
public class PaymentDAO {
    private final JdbcTemplate jdbc;

    public PaymentDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Payment findById(int id) {
        List<Payment> rows = jdbc.query("select * from PAYMENT where payment_id = ?", this::read, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public List<Payment> findAll() {
        return jdbc.query("select * from PAYMENT order by payment_id", this::read);
    }

    public int count() {
        return jdbc.queryForObject("select count(*) from PAYMENT", Integer.class);
    }

    public void delete(int id) {
        jdbc.update("delete from PAYMENT where payment_id = ?", id);
    }

    public List<Payment> findByClaim(int claimId) {
        return jdbc.query("select * from PAYMENT where claim_id = ? order by payment_id",
                this::read, claimId);
    }

    public double totalIssued(int claimId) {
        return jdbc.queryForObject("select coalesce(sum(amount),0) from PAYMENT where claim_id = ?",
                Double.class, claimId);
    }

    public int nextId() {
        return jdbc.queryForObject("select coalesce(max(payment_id),0)+1 from PAYMENT", Integer.class);
    }

    public void insert(Payment value) {
        jdbc.update(connection -> {
            var ps = connection.prepareStatement(
                    "insert into PAYMENT "
                    + "(payment_id,claim_id,settlement_id,payee_name,amount,"
                    + "payment_method,check_number,issued_date,status) "
                    + "values (?,?,?,?,?,?,?,?,?)");
            ps.setInt(1, value.getPaymentId());
            ps.setInt(2, value.getClaimId());
            ps.setInt(3, value.getSettlementId());
            ps.setString(4, value.getPayeeName());
            ps.setDouble(5, value.getAmount());
            ps.setString(6, value.getPaymentMethod());
            ps.setString(7, value.getCheckNumber());
            ps.setString(8, value.getIssuedDate());
            ps.setString(9, value.getStatus());
            return ps;
        });
    }

    private Payment read(ResultSet rs, int row) throws SQLException {
        Payment value = new Payment();
        value.setPaymentId(rs.getInt("payment_id"));
        value.setClaimId(rs.getInt("claim_id"));
        value.setSettlementId(rs.getInt("settlement_id"));
        value.setPayeeName(rs.getString("payee_name"));
        value.setAmount(rs.getDouble("amount"));
        value.setPaymentMethod(rs.getString("payment_method"));
        value.setCheckNumber(rs.getString("check_number"));
        value.setIssuedDate(rs.getString("issued_date"));
        value.setStatus(rs.getString("status"));
        return value;
    }
}
