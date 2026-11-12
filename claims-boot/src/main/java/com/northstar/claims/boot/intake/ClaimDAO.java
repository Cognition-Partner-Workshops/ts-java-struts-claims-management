package com.northstar.claims.boot.intake;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Legacy SQL is deliberately retained, including the action-specific update paths. */
@Repository
public class ClaimDAO {
    private final JdbcTemplate jdbc;

    public ClaimDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Claim findById(int id) {
        List<Claim> rows = jdbc.query("select * from CLAIM where claim_id = ?", this::read, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public List<Claim> findAll() {
        return jdbc.query("select * from CLAIM order by claim_id", this::read);
    }

    public List<Claim> findByStatus(String status) {
        return jdbc.query("select * from CLAIM where status = ? order by claim_id", this::read, status);
    }

    public int submit(IntakeForm form) {
        int id = jdbc.queryForObject("select coalesce(max(claim_id),0)+1 from CLAIM", Integer.class);
        String date = form.normalizedDate();
        jdbc.update("insert into CLAIM values (?,? ,9001,?,?,?,?,?,'OPEN',?,"
                        + "'adjuster1','supervisor',?)", id, "CLM-" + id, form.claimantName(),
                date, date, "WATER", form.description(), form.initialReserve(), date);
        return id;
    }

    public void assign(int id, String adjuster) {
        jdbc.update("update CLAIM set assigned_adjuster = ? where claim_id = ?", adjuster, id);
    }

    public void updateReserve(int id, double reserve) {
        jdbc.update("update CLAIM set reserve_amount = ? where claim_id = ?", reserve, id);
    }

    public void updateStatus(int id, String status) {
        jdbc.update("update CLAIM set status = ? where claim_id = ?", status, id);
    }

    public void updateClosure(int id, boolean closed, String user, String asOf) {
        // These are separate autocommit statements in Struts: do not add a transaction.
        if (closed) {
            jdbc.update("insert into CLAIM_CLOSURE select ?, cast(? as date), ? "
                    + "from (values(0)) where not exists (select 1 "
                    + "from CLAIM_CLOSURE where claim_id = ?)", id, asOf, user, id);
        } else {
            jdbc.update("delete from CLAIM_CLOSURE where claim_id = ?", id);
        }
    }

    private Claim read(ResultSet rs, int row) throws SQLException {
        return new Claim(rs.getInt("claim_id"), rs.getString("claim_number"), rs.getInt("policy_id"),
                rs.getString("claimant_name"), rs.getString("loss_date"), rs.getString("reported_date"),
                rs.getString("loss_type"), rs.getString("description"), rs.getString("status"),
                rs.getDouble("reserve_amount"), rs.getString("assigned_adjuster"),
                rs.getString("created_by"), rs.getString("created_date"));
    }
}
