package com.mandateguard.simulator.service;

import com.mandateguard.simulator.model.Agent;
import com.mandateguard.simulator.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReputationTracker {

    private final Map<UUID, Double> reputationScores = new HashMap<>();
    private final Map<UUID, Double> reputationDeltas = new HashMap<>();

    public void initialize(Agent agent) {
        reputationScores.put(agent.agentId(), agent.reputationScore().doubleValue());
        reputationDeltas.put(agent.agentId(), 0.0);
    }

    public void recordSuccessfulPayment(UUID agentId, double amount) {
        double current = reputationScores.getOrDefault(agentId, 0.5);
        double delta = Math.min(0.05, amount * 0.001);
        double newScore = Math.min(1.0, current + delta);
        reputationScores.put(agentId, newScore);
        reputationDeltas.merge(agentId, delta, Double::sum);
    }

    public void recordFraudFlag(UUID agentId) {
        double current = reputationScores.getOrDefault(agentId, 0.5);
        double penalty = 0.2;
        double newScore = Math.max(0.0, current - penalty);
        reputationScores.put(agentId, newScore);
        reputationDeltas.merge(agentId, -penalty, Double::sum);
    }

    public void recordFailedPayment(UUID agentId) {
        double current = reputationScores.getOrDefault(agentId, 0.5);
        double penalty = 0.05;
        double newScore = Math.max(0.0, current - penalty);
        reputationScores.put(agentId, newScore);
        reputationDeltas.merge(agentId, -penalty, Double::sum);
    }

    public double getScore(UUID agentId) {
        return reputationScores.getOrDefault(agentId, 0.5);
    }

    public double getDelta(UUID agentId) {
        return reputationDeltas.getOrDefault(agentId, 0.0);
    }

    public Map<UUID, Double> getAllScores() {
        return Collections.unmodifiableMap(reputationScores);
    }
}
