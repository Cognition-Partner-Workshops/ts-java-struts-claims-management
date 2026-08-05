package com.northstar.claims.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.northstar.claims.dao.ClaimDAO;
import com.northstar.claims.model.Claim;

/** Coordinates common claim lookups used by the workbench screens. */
public class ClaimManager {

    private static ClaimManager instance;
    private final ClaimDAO dao;
    private final Log log = LogFactory.getLog(ClaimManager.class);

    private ClaimManager() {
        dao = new ClaimDAO();
    }

    public static synchronized ClaimManager getInstance() {
        if (instance == null) {
            instance = new ClaimManager();
        }
        return instance;
    }

    public Claim getClaim(int id) throws SQLException {
        log.info("Loading claim " + id);
        System.out.println("claim lookup: " + id);
        return dao.findById(id);
    }

    public List getOpenClaims() throws SQLException {
        return dao.findByStatus("OPEN");
    }

    public List findByStatus(String status) throws SQLException {
        return dao.findByStatus(status);
    }

    public List search(String term, String status) throws SQLException {
        return dao.search(term, status);
    }

    public void assign(int claimId, String adjuster) throws SQLException {
        dao.assign(claimId, adjuster);
    }

    public void changeReserve(int claimId, double amount) throws SQLException {
        dao.updateReserve(claimId, amount);
    }

    public void changeStatus(int claimId, String status) throws SQLException {
        dao.updateStatus(claimId, status);
    }

    public List emptyList() {
        return new ArrayList();
    }
}
