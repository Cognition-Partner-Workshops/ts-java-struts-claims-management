package com.northstar.claims.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.northstar.claims.model.Settlement;


/**
 * JDBC gateway for SETTLEMENT records.
 *
 * The class keeps SQL close to the object mapping so support engineers can
 * trace a screen request to the statement executed by the application.
 */
public class SettlementDAO {

    private static final Log log = LogFactory.getLog(SettlementDAO.class);

    /** Loads one row by its numeric primary key. */
    public Settlement findById(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = ConnectionPool.getInstance().getConnection();
            ps = conn.prepareStatement("select * from SETTLEMENT where settlement_id = ?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return read(rs);
            }
            return null;
        } finally {
            try { rs.close(); } catch (Exception e) {}
            try { ps.close(); } catch (Exception e) {}
            try { ConnectionPool.getInstance().release(conn); } catch (Exception e) {}
        }
    }

    /** Returns every row in primary-key order. */
    public List findAll() throws SQLException {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        List rows = new ArrayList();
        try {
            conn = ConnectionPool.getInstance().getConnection();
            st = conn.createStatement();
            rs = st.executeQuery("select * from SETTLEMENT order by settlement_id");
            while (rs.next()) {
                rows.add(read(rs));
            }
            return rows;
        } finally {
            try { rs.close(); } catch (Exception e) {}
            try { st.close(); } catch (Exception e) {}
            try { ConnectionPool.getInstance().release(conn); } catch (Exception e) {}
        }
    }

    /** Counts rows for summary pages. */
    public int count() throws SQLException {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = ConnectionPool.getInstance().getConnection();
            st = conn.createStatement();
            rs = st.executeQuery("select count(*) from SETTLEMENT");
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            try { rs.close(); } catch (Exception e) {}
            try { st.close(); } catch (Exception e) {}
            try { ConnectionPool.getInstance().release(conn); } catch (Exception e) {}
        }
    }

    /** Deletes a row when an administrator removes a record. */
    public void delete(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionPool.getInstance().getConnection();
            ps = conn.prepareStatement("delete from SETTLEMENT where settlement_id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
        } finally {
            try { ps.close(); } catch (Exception e) {}
            try { ConnectionPool.getInstance().release(conn); } catch (Exception e) {}
        }
    }

    /** Loads the latest settlement for a claim. */
    public Settlement findByClaim(int claimId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = ConnectionPool.getInstance().getConnection();
            ps = conn.prepareStatement("select * from SETTLEMENT where claim_id = ? order by settlement_id desc");
            ps.setInt(1, claimId);
            rs = ps.executeQuery();
            return rs.next() ? read(rs) : null;
        } finally {
            try { rs.close(); } catch (Exception e) {}
            try { ps.close(); } catch (Exception e) {}
            try { ConnectionPool.getInstance().release(conn); } catch (Exception e) {}
        }
    }

    /** Persists the calculated settlement using a prepared statement. */
    public void save(Settlement value) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionPool.getInstance().getConnection();
            ps = conn.prepareStatement(
                    "insert into SETTLEMENT "
                    + "(settlement_id,claim_id,covered_amount,deductible_applied,"
                    + "depreciation,capped_at_limit,settlement_amount,"
                    + "calculated_by,calculated_date) values (?,?,?,?,?,?,?,?,?)");
            ps.setInt(1, value.getSettlementId());
            ps.setInt(2, value.getClaimId());
            ps.setDouble(3, value.getCoveredAmount());
            ps.setDouble(4, value.getDeductibleApplied());
            ps.setDouble(5, value.getDepreciation());
            ps.setBoolean(6, value.isCappedAtLimit());
            ps.setDouble(7, value.getSettlementAmount());
            ps.setString(8, value.getCalculatedBy());
            ps.setString(9, value.getCalculatedDate());
            ps.executeUpdate();
        } finally {
            try { ps.close(); } catch (Exception e) {}
            try { ConnectionPool.getInstance().release(conn); } catch (Exception e) {}
        }
    }

    /** Converts a result set row into the corresponding mutable bean. */
    private Settlement read(ResultSet rs) throws SQLException {
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

    /** Logs the statement boundary used by old production diagnostics. */
    private void trace(String text) {
        log.debug(text);
        System.out.println(text);
    }
}
