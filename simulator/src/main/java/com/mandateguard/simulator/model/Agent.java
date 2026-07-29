package com.mandateguard.simulator.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Agent(
    UUID agentId,
    AgentType type,
    AgentProfile profile,
    Instant createdAt,
    BigDecimal reputationScore,
    double reputationDelta,
    BigDecimal spendBaseline
) {
    public enum AgentType {
        NORMAL, FRAUD_SYBIL, FRAUD_COLLUSION, FRAUD_REPLAY, FRAUD_DOS
    }

    public static Agent create(AgentType type) {
        return create(type, AgentProfile.random(new java.util.Random()));
    }

    public static Agent create(AgentType type, AgentProfile profile) {
        return new Agent(
            UUID.randomUUID(),
            type,
            profile,
            Instant.now(),
            BigDecimal.valueOf(0.5 + Math.random() * 0.5),
            0.0,
            BigDecimal.valueOf(profile.getAvgAmount() * (0.5 + Math.random()))
        );
    }
}
