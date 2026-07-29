package com.mandateguard.policy.model;

import java.time.Instant;
import java.util.UUID;

public record MandateHold(
    UUID holdId,
    UUID mandateId,
    UUID agentId,
    String status,
    String reason,
    Instant heldAt
) {
    public static MandateHold create(UUID mandateId, UUID agentId, String reason) {
        return new MandateHold(UUID.randomUUID(), mandateId, agentId, "HELD", reason, Instant.now());
    }
}
