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
import com.northstar.claims.model.ClaimNote;


/**
 * JDBC gateway for CLAIM_NOTE records.
 *
 * The class keeps SQL close to the object mapping so support engineers can
 * trace a screen request to the statement executed by the application.
 */
public class NoteDAO {

    private static final Log log = LogFactory.getLog(NoteDAO.class);

    /** Loads one row by its numeric primary key. */
    public ClaimNote findById(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = ConnectionPool.getInstance().getConnection();
            ps = conn.prepareStatement("select * from CLAIM_NOTE where note_id = ?");
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
            rs = st.executeQuery("select * from CLAIM_NOTE order by note_id");
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
            rs = st.executeQuery("select count(*) from CLAIM_NOTE");
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
            ps = conn.prepareStatement("delete from CLAIM_NOTE where note_id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
        } finally {
            try { ps.close(); } catch (Exception e) {}
            try { ConnectionPool.getInstance().release(conn); } catch (Exception e) {}
        }
    }

    /** Returns notes attached to a claim. */
    public List findByClaim(int claimId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List rows = new ArrayList();
        try {
            conn = ConnectionPool.getInstance().getConnection();
            ps = conn.prepareStatement("select * from CLAIM_NOTE where claim_id = ? order by note_date");
            ps.setInt(1, claimId);
            rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(read(rs));
            }
            return rows;
        } finally {
            try { rs.close(); } catch (Exception e) {}
            try { ps.close(); } catch (Exception e) {}
            try { ConnectionPool.getInstance().release(conn); } catch (Exception e) {}
        }
    }

    /** Retained for the note maintenance screen even though no current action calls it. */
    public ClaimNote findUnused(int id) throws SQLException {
        List notes = findByClaim(id);
        if (notes.isEmpty()) {
            return null;
        }
        return (ClaimNote) notes.get(0);
    }

    /** Inserts an operator note. */
    public void insert(ClaimNote value) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionPool.getInstance().getConnection();
            ps = conn.prepareStatement("insert into CLAIM_NOTE values (?,?,?,?,?)");
            ps.setInt(1, value.getNoteId());
            ps.setInt(2, value.getClaimId());
            ps.setString(3, value.getAuthor());
            ps.setString(4, value.getNoteDate());
            ps.setString(5, value.getNoteText());
            ps.executeUpdate();
        } finally {
            try { ps.close(); } catch (Exception e) {}
            try { ConnectionPool.getInstance().release(conn); } catch (Exception e) {}
        }
    }

    /** Converts a result set row into the corresponding mutable bean. */
    private ClaimNote read(ResultSet rs) throws SQLException {
        ClaimNote value = new ClaimNote();
        value.setNoteId(rs.getInt("note_id"));
        value.setClaimId(rs.getInt("claim_id"));
        value.setAuthor(rs.getString("author"));
        value.setNoteDate(rs.getString("note_date"));
        value.setNoteText(rs.getString("note_text"));
        return value;
    }

    /** Logs the statement boundary used by old production diagnostics. */
    private void trace(String text) {
        log.debug(text);
        System.out.println(text);
    }
}
