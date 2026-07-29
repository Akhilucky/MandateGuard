package com.mandateguard.simulator.model;

import java.util.Set;

public record FraudScenario(
    String id,
    String name,
    String description,
    Set<String> enabledFraudTypes,
    double fraudRate
) {
    public static FraudScenario SCENARIO_A = new FraudScenario(
        "A", "Replay Only", "Only mandate replay attacks",
        Set.of("replay"), 0.02
    );

    public static FraudScenario SCENARIO_B = new FraudScenario(
        "B", "Replay + Sybil", "Replay and sybil cluster attacks",
        Set.of("replay", "sybil"), 0.03
    );

    public static FraudScenario SCENARIO_C = new FraudScenario(
        "C", "Full Spectrum", "All fraud types active",
        Set.of("replay", "sybil", "collusion", "dos", "velocity"), 0.03
    );

    public static FraudScenario SCENARIO_D = new FraudScenario(
        "D", "Targeted", "High-rate attack on specific profiles",
        Set.of("replay", "sybil", "collusion", "dos", "velocity"), 0.05
    );

    public static FraudScenario byId(String id) {
        return switch (id) {
            case "A" -> SCENARIO_A;
            case "B" -> SCENARIO_B;
            case "C" -> SCENARIO_C;
            case "D" -> SCENARIO_D;
            default -> SCENARIO_C;
        };
    }
}
