package com.mandateguard.simulator.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Agent(
    UUID agentId,
    AgentType type,
    Instant createdAt,
    BigDecimal reputationScore,
    BigDecimal spendBaseline
) {
    public enum AgentType {
        NORMAL, FRAUD_SYBIL, FRAUD_COLLUSION, FRAUD_REPLAY, FRAUD_DOS
    }

    public static Agent create(AgentType type) {
        return new Agent(
            UUID.randomUUID(),
            type,
            Instant.now(),
            BigDecimal.valueOf(0.5 + Math.random() * 0.5),
            BigDecimal.valueOf(0.01 + Math.random() * 5.0)
        );
    }
}
