package com.mandateguard.simulator.model;

import java.math.BigDecimal;

public enum AgentProfile {
    TRANSLATION("translation-service", 3.0, 0.5, 20.0, 0.3),
    COMPUTE("compute-rental", 8.0, 5.0, 50.0, 0.6),
    SHOPPING("shopping-agent", 2.0, 15.0, 100.0, 0.2),
    SEARCH("search-agent", 10.0, 0.1, 5.0, 0.7),
    DATA_BROKER("data-broker", 1.0, 50.0, 200.0, 0.4),
    API_PROVIDER("api-provider", 15.0, 0.2, 10.0, 0.8),
    STORAGE("storage-provider", 0.5, 10.0, 30.0, 0.5),
    MODEL_PROVIDER("model-provider", 5.0, 2.0, 25.0, 0.5);

    private final String scope;
    private final double avgTxPerHour;
    private final double avgAmount;
    private final double maxAmount;
    private final double burstProbability;

    AgentProfile(String scope, double avgTxPerHour, double avgAmount, double maxAmount, double burstProbability) {
        this.scope = scope;
        this.avgTxPerHour = avgTxPerHour;
        this.avgAmount = avgAmount;
        this.maxAmount = maxAmount;
        this.burstProbability = burstProbability;
    }

    public String getScope() { return scope; }
    public double getAvgTxPerHour() { return avgTxPerHour; }
    public double getAvgAmount() { return avgAmount; }
    public double getMaxAmount() { return maxAmount; }
    public double getBurstProbability() { return burstProbability; }

    public static AgentProfile random(java.util.Random rng) {
        AgentProfile[] values = values();
        return values[rng.nextInt(values.length)];
    }
}
