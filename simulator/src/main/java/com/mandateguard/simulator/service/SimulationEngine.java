package com.mandateguard.simulator.service;

import com.mandateguard.simulator.config.SimulatorConfig;
import com.mandateguard.simulator.model.Agent;
import com.mandateguard.simulator.model.Agent.AgentType;
import com.mandateguard.simulator.model.Mandate;
import com.mandateguard.simulator.model.Transaction;
import com.mandateguard.simulator.fraud.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class SimulationEngine {

    private final SimulatorConfig config;
    private final JdbcTemplate jdbc;
    private final List<Agent> agents = new ArrayList<>();
    private final Queue<Transaction> transactionBuffer = new ConcurrentLinkedQueue<>();
    private final Map<UUID, List<Mandate>> agentMandates = new HashMap<>();
    private final Random random = new Random(42);

    private final MandateReplayFraud mandateReplayFraud;
    private final MicropaymentDosFraud micropaymentDosFraud;
    private final SybilClusterFraud sybilClusterFraud;
    private final CollusionRingFraud collusionRingFraud;
    private final VelocityAnomalyFraud velocityAnomalyFraud;

    public SimulationEngine(SimulatorConfig config, JdbcTemplate jdbc,
                            MandateReplayFraud mandateReplayFraud,
                            MicropaymentDosFraud micropaymentDosFraud,
                            SybilClusterFraud sybilClusterFraud,
                            CollusionRingFraud collusionRingFraud,
                            VelocityAnomalyFraud velocityAnomalyFraud) {
        this.config = config;
        this.jdbc = jdbc;
        this.mandateReplayFraud = mandateReplayFraud;
        this.micropaymentDosFraud = micropaymentDosFraud;
        this.sybilClusterFraud = sybilClusterFraud;
        this.collusionRingFraud = collusionRingFraud;
        this.velocityAnomalyFraud = velocityAnomalyFraud;
    }

    public Map<String, Object> runSimulation() {
        long startTime = System.currentTimeMillis();

        transactionBuffer.clear();
        agents.clear();
        agentMandates.clear();

        generatePopulation();
        generateNormalTraffic();
        injectFraud();
        persistTransactions();

        long elapsed = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("totalAgents", agents.size());
        result.put("totalTransactions", transactionBuffer.size());
        result.put("simulationTimeMs", elapsed);
        result.put("fraudRate", config.getFraudRate());
        return result;
    }

    private void generatePopulation() {
        int fraudCount = (int)(config.getPopulationSize() * config.getFraudRate());

        for (int i = 0; i < config.getPopulationSize() - fraudCount; i++) {
            agents.add(Agent.create(AgentType.NORMAL));
        }

        AgentType[] fraudTypes = {
            AgentType.FRAUD_SYBIL, AgentType.FRAUD_COLLUSION,
            AgentType.FRAUD_REPLAY, AgentType.FRAUD_DOS
        };
        for (int i = 0; i < fraudCount; i++) {
            AgentType type = fraudTypes[i % fraudTypes.length];
            agents.add(Agent.create(type));
        }
        Collections.shuffle(agents, random);
    }

    private void generateNormalTraffic() {
        Instant windowStart = Instant.now().minus(config.getTimeWindowHours(), ChronoUnit.HOURS);
        long windowSeconds = ChronoUnit.HOURS.getDuration(config.getTimeWindowHours()).getSeconds();

        for (Agent agent : agents) {
            if (agent.type() != AgentType.NORMAL) continue;

            Mandate mandate = Mandate.create(agent.agentId());
            agentMandates.computeIfAbsent(agent.agentId(), k -> new ArrayList<>()).add(mandate);

            double activityLevel = samplePowerLaw(0.5, 3.0);
            int txCount = Math.max(1, (int)(config.getAvgTransactionsPerHour() * config.getTimeWindowHours() * activityLevel));

            for (int i = 0; i < txCount; i++) {
                Agent recipient = pickWeightedRecipient(agent.agentId());
                if (recipient == null) continue;

                double amount = sampleLogNormal(agent.spendBaseline().doubleValue());
                Instant txTime = sampleTimeWithDiurnalPattern(windowStart, windowSeconds);

                transactionBuffer.add(Transaction.create(
                    mandate.mandateId(), agent.agentId(), recipient.agentId(),
                    BigDecimal.valueOf(amount).setScale(6, java.math.RoundingMode.HALF_UP), false
                ));
            }
        }
    }

    private double samplePowerLaw(double min, double max) {
        double alpha = 2.5;
        double u = random.nextDouble();
        double scaled = min * Math.pow(u, -1.0 / (alpha - 1.0));
        return Math.min(scaled, max);
    }

    private double sampleLogNormal(double baseline) {
        double mu = Math.log(Math.max(baseline, 0.01));
        double sigma = 0.8;
        double sample = random.nextGaussian() * sigma + mu;
        return Math.max(0.001, Math.exp(sample));
    }

    private Instant sampleTimeWithDiurnalPattern(Instant windowStart, long windowSeconds) {
        double hourOfDay = (random.nextDouble() * 24);
        double activityWeight = 0.3 + 0.7 * Math.exp(-Math.pow(hourOfDay - 14, 2) / 50.0);
        if (random.nextDouble() > activityWeight) {
            hourOfDay = random.nextDouble() * 24;
        }
        long offsetSeconds = (long)(hourOfDay * 3600) % windowSeconds;
        return windowStart.plusSeconds(offsetSeconds);
    }

    private Agent pickWeightedRecipient(UUID excludeId) {
        List<Agent> candidates = agents.stream()
            .filter(a -> !a.agentId().equals(excludeId))
            .toList();
        if (candidates.isEmpty()) return null;

        double[] weights = new double[candidates.size()];
        double totalWeight = 0;
        for (int i = 0; i < candidates.size(); i++) {
            weights[i] = Math.pow(candidates.get(i).reputationScore().doubleValue(), 2.0);
            totalWeight += weights[i];
        }

        double r = random.nextDouble() * totalWeight;
        double cumulative = 0;
        for (int i = 0; i < candidates.size(); i++) {
            cumulative += weights[i];
            if (r <= cumulative) return candidates.get(i);
        }
        return candidates.get(candidates.size() - 1);
    }

    private void injectFraud() {
        List<Agent> sybilAgents = agents.stream().filter(a -> a.type() == AgentType.FRAUD_SYBIL).toList();
        List<Agent> collusionAgents = agents.stream().filter(a -> a.type() == AgentType.FRAUD_COLLUSION).toList();
        List<Agent> replayAgents = agents.stream().filter(a -> a.type() == AgentType.FRAUD_REPLAY).toList();
        List<Agent> dosAgents = agents.stream().filter(a -> a.type() == AgentType.FRAUD_DOS).toList();

        if (!sybilAgents.isEmpty()) sybilClusterFraud.inject(sybilAgents, transactionBuffer);
        if (!collusionAgents.isEmpty()) collusionRingFraud.inject(collusionAgents, transactionBuffer);
        if (!replayAgents.isEmpty()) mandateReplayFraud.inject(replayAgents, agentMandates, transactionBuffer);
        if (!dosAgents.isEmpty()) micropaymentDosFraud.inject(dosAgents, transactionBuffer);
        velocityAnomalyFraud.inject(agents, agentMandates, transactionBuffer);
    }

    private void persistTransactions() {
        String sql = "INSERT INTO transactions (tx_id, mandate_id, from_agent_id, to_agent_id, amount, currency, timestamp, is_fraud_label) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        for (Transaction tx : transactionBuffer) {
            jdbc.update(sql,
                tx.txId(), tx.mandateId(), tx.fromAgentId(), tx.toAgentId(),
                tx.amount(), tx.currency(), tx.timestamp(), tx.isFraudLabel()
            );
        }
    }
}
