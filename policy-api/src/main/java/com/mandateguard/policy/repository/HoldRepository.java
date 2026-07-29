package com.mandateguard.policy.repository;

import com.mandateguard.policy.model.MandateHold;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class HoldRepository {

    private final JdbcTemplate jdbc;

    public HoldRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(MandateHold hold) {
        String sql = "INSERT INTO mandate_holds (hold_id, mandate_id, agent_id, status, reason, held_at) VALUES (?, ?, ?, ?, ?, ?)";
        jdbc.update(sql, hold.holdId(), hold.mandateId(), hold.agentId(), hold.status(), hold.reason(), hold.heldAt());
    }

    public List<MandateHold> findAll() {
        String sql = "SELECT * FROM mandate_holds ORDER BY held_at DESC";
        return jdbc.query(sql, (rs, rowNum) -> new MandateHold(
            rs.getObject("hold_id", java.util.UUID.class),
            rs.getObject("mandate_id", java.util.UUID.class),
            rs.getObject("agent_id", java.util.UUID.class),
            rs.getString("status"),
            rs.getString("reason"),
            rs.getTimestamp("held_at").toInstant()
        ));
    }
}
