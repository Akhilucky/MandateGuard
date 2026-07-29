package com.mandateguard.ledger.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionRecord(
    UUID txId,
    UUID mandateId,
    UUID fromAgentId,
    UUID toAgentId,
    BigDecimal amount,
    String currency,
    Instant timestamp,
    boolean isFraudLabel
) {}
