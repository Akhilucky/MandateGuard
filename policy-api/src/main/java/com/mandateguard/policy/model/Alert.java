package com.mandateguard.policy.model;

import java.time.Instant;
import java.util.UUID;

public record Alert(
    UUID alertId,
    UUID txId,
    UUID agentId,
    String riskLevel,
    double riskScore,
    String reason,
    Instant createdAt,
    boolean resolved
) {
    public static Alert create(UUID txId, UUID agentId, double riskScore, String reason) {
        String level = riskScore > 0.8 ? "CRITICAL" : riskScore > 0.5 ? "HIGH" : riskScore > 0.3 ? "MEDIUM" : "LOW";
        return new Alert(UUID.randomUUID(), txId, agentId, level, riskScore, reason, Instant.now(), false);
    }
}
