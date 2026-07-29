package com.mandateguard.simulator.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Mandate(
    UUID mandateId,
    UUID issuerAgentId,
    String scope,
    BigDecimal maxAmount,
    Instant expiry,
    String signature
) {
    public static Mandate create(UUID issuerAgentId) {
        return create(issuerAgentId, pickScope());
    }

    public static Mandate create(UUID issuerAgentId, String scope) {
        Instant now = Instant.now();
        return new Mandate(
            UUID.randomUUID(), issuerAgentId, scope,
            BigDecimal.valueOf(0.1 + Math.random() * 100.0),
            now.plusSeconds(3600 + (long)(Math.random() * 86400)),
            "mock-sig-" + UUID.randomUUID().toString().substring(0, 8)
        );
    }

    private static String pickScope() {
        String[] scopes = {"translation-service", "compute-rental", "data-query", "image-gen", "code-review"};
        return scopes[(int)(Math.random() * scopes.length)];
    }
}
