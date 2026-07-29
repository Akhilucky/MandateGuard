package com.mandateguard.simulator.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transaction(
    UUID txId,
    UUID mandateId,
    UUID fromAgentId,
    UUID toAgentId,
    BigDecimal amount,
    String currency,
    Instant timestamp,
    boolean isFraudLabel,
    String fraudType
) {
    public static Transaction create(UUID mandateId, UUID from, UUID to, BigDecimal amount, boolean isFraud) {
        return create(mandateId, from, to, amount, isFraud, "normal");
    }

    public static Transaction create(UUID mandateId, UUID from, UUID to, BigDecimal amount, boolean isFraud, String fraudType) {
        return new Transaction(
            UUID.randomUUID(), mandateId, from, to, amount, "USDC", Instant.now(), isFraud, fraudType
        );
    }
}
