package com.mandateguard.simulator.model;

import java.time.Instant;
import java.util.UUID;

public record TrustEdge(
    UUID trustId,
    UUID fromAgentId,
    UUID toAgentId,
    double trustScore,
    Instant establishedAt,
    boolean active
) {
    public static TrustEdge create(UUID from, UUID to, double initialTrust) {
        return new TrustEdge(UUID.randomUUID(), from, to, initialTrust, Instant.now(), true);
    }
}
