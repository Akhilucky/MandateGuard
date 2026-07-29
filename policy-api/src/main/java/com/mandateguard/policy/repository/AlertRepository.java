package com.mandateguard.policy.repository;

import com.mandateguard.policy.model.Alert;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class AlertRepository {

    private final JdbcTemplate jdbc;

    public AlertRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(Alert alert) {
        String sql = "INSERT INTO alerts (alert_id, tx_id, agent_id, risk_level, risk_score, reason, created_at, resolved) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbc.update(sql, alert.alertId(), alert.txId(), alert.agentId(), alert.riskLevel(), alert.riskScore(), alert.reason(), alert.createdAt(), alert.resolved());
    }

    public List<Alert> findUnresolved() {
        String sql = "SELECT * FROM alerts WHERE resolved = false ORDER BY created_at DESC";
        return jdbc.query(sql, (rs, rowNum) -> new Alert(
            rs.getObject("alert_id", java.util.UUID.class),
            rs.getObject("tx_id", java.util.UUID.class),
            rs.getObject("agent_id", java.util.UUID.class),
            rs.getString("risk_level"),
            rs.getDouble("risk_score"),
            rs.getString("reason"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getBoolean("resolved")
        ));
    }

    public List<Alert> findAll(int limit) {
        String sql = "SELECT * FROM alerts ORDER BY created_at DESC LIMIT ?";
        return jdbc.query(sql, (rs, rowNum) -> new Alert(
            rs.getObject("alert_id", java.util.UUID.class),
            rs.getObject("tx_id", java.util.UUID.class),
            rs.getObject("agent_id", java.util.UUID.class),
            rs.getString("risk_level"),
            rs.getDouble("risk_score"),
            rs.getString("reason"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getBoolean("resolved")
        ), limit);
    }

    public void resolve(UUID alertId) {
        jdbc.update("UPDATE alerts SET resolved = true WHERE alert_id = ?", alertId);
    }
}
