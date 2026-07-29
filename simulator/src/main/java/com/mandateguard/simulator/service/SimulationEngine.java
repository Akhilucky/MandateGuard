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
        agents.clear();
        agentMandates.clear();
        int fraudCount = (int)(config.getPopulationSize() * config.getFraudRate());

        for (int i = 0; i < config.getPopulationSize() - fraudCount; i++) {
            Agent agent = Agent.create(AgentType.NORMAL);
            agents.add(agent);
        }

        AgentType[] fraudTypes = {
            AgentType.FRAUD_SYBIL, AgentType.FRAUD_COLLUSION,
            AgentType.FRAUD_REPLAY, AgentType.FRAUD_DOS
        };
        for (int i = 0; i < fraudCount; i++) {
            AgentType type = fraudTypes[i % fraudTypes.length];
            agents.add(Agent.create(type));
        }
        Collections.shuffle(agents);
    }

    private void generateNormalTraffic() {
        Instant windowStart = Instant.now().minus(config.getTimeWindowHours(), ChronoUnit.HOURS);
        Instant windowEnd = Instant.now();

        for (Agent agent : agents) {
            if (agent.type() != AgentType.NORMAL) continue;

            Mandate mandate = Mandate.create(agent.agentId());
            agentMandates.computeIfAbsent(agent.agentId(), k -> new ArrayList<>()).add(mandate);

            int txCount = (int)(config.getAvgTransactionsPerHour() * config.getTimeWindowHours() * (0.5 + Math.random()));
            for (int i = 0; i < txCount; i++) {
                Agent recipient = pickRandomAgentExcluding(agent.agentId());
                if (recipient == null) continue;

                BigDecimal amount = BigDecimal.valueOf(0.01 + Math.random() * agent.spendBaseline().doubleValue());
                Instant txTime = windowStart.plusSeconds((long)(Math.random() * ChronoUnit.HOURS.getDuration(config.getTimeWindowHours()).getSeconds()));

                transactionBuffer.add(Transaction.create(
                    mandate.mandateId(), agent.agentId(), recipient.agentId(),
                    amount.setScale(6, java.math.RoundingMode.HALF_UP), false
                ));
            }
        }
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

    private Agent pickRandomAgentExcluding(UUID excludeId) {
        List<Agent> candidates = agents.stream().filter(a -> !a.agentId().equals(excludeId)).toList();
        return candidates.isEmpty() ? null : candidates.get((int)(Math.random() * candidates.size()));
    }
}
