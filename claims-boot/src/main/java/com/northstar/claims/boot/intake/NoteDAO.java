package com.northstar.claims.boot.intake;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NoteDAO {
    private final JdbcTemplate jdbc;

    public NoteDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ClaimNote> findByClaim(int claimId) {
        return jdbc.query("select * from CLAIM_NOTE where claim_id = ? order by note_date",
                (rs, row) -> new ClaimNote(rs.getInt("note_id"), rs.getInt("claim_id"),
                        rs.getString("author"), rs.getString("note_date"), rs.getString("note_text")), claimId);
    }
}
