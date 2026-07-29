package com.mandateguard.simulator.service;

import com.mandateguard.simulator.config.SimulatorConfig;
import com.mandateguard.simulator.model.*;
import com.mandateguard.simulator.model.Agent.AgentType;
import com.mandateguard.simulator.fraud.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class SimulationEngine {

    private final SimulatorConfig config;
    private final JdbcTemplate jdbc;
    private final MandateReplayFraud mandateReplayFraud;
    private final MicropaymentDosFraud micropaymentDosFraud;
    private final SybilClusterFraud sybilClusterFraud;
    private final CollusionRingFraud collusionRingFraud;
    private final VelocityAnomalyFraud velocityAnomalyFraud;
    private final ReputationTracker reputationTracker;
    private final TrustGraphService trustGraphService;
    private final DatasetExporter datasetExporter;

    private final Random random = new Random(42);
    private List<Agent> agents = new ArrayList<>();
    private Queue<Transaction> transactionBuffer = new ConcurrentLinkedQueue<>();
    private Map<UUID, List<Mandate>> agentMandates = new HashMap<>();

    public SimulationEngine(SimulatorConfig config, JdbcTemplate jdbc,
                            MandateReplayFraud mandateReplayFraud,
                            MicropaymentDosFraud micropaymentDosFraud,
                            SybilClusterFraud sybilClusterFraud,
                            CollusionRingFraud collusionRingFraud,
                            VelocityAnomalyFraud velocityAnomalyFraud,
                            ReputationTracker reputationTracker,
                            TrustGraphService trustGraphService,
                            DatasetExporter datasetExporter) {
        this.config = config;
        this.jdbc = jdbc;
        this.mandateReplayFraud = mandateReplayFraud;
        this.micropaymentDosFraud = micropaymentDosFraud;
        this.sybilClusterFraud = sybilClusterFraud;
        this.collusionRingFraud = collusionRingFraud;
        this.velocityAnomalyFraud = velocityAnomalyFraud;
        this.reputationTracker = reputationTracker;
        this.trustGraphService = trustGraphService;
        this.datasetExporter = datasetExporter;
    }

    public Map<String, Object> runSimulation() {
        return runSimulation(null, null);
    }

    public Map<String, Object> runSimulation(String scenarioId, Integer populationOverride) {
        long startTime = System.currentTimeMillis();

        transactionBuffer.clear();
        agents.clear();
        agentMandates.clear();

        FraudScenario scenario = scenarioId != null ? FraudScenario.byId(scenarioId) : FraudScenario.SCENARIO_C;
        int popSize = populationOverride != null ? populationOverride : config.getPopulationSize();

        generatePopulation(popSize, scenario);
        trustGraphService.initializeTrustGraph(agents, random);
        for (Agent agent : agents) {
            reputationTracker.initialize(agent);
        }

        generateNormalTraffic();
        injectFraud(scenario);
        updateReputation();
        persistTransactions();

        long elapsed = System.currentTimeMillis() - startTime;

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("scenario", scenario.id());
        metadata.put("scenario_name", scenario.name());
        metadata.put("population_size", agents.size());
        metadata.put("total_transactions", transactionBuffer.size());
        metadata.put("simulation_time_ms", elapsed);
        metadata.put("fraud_rate", scenario.fraudRate());
        metadata.put("trust_edges", trustGraphService.getTrustEdges().size());

        Map<String, String> dataset = datasetExporter.exportDataset(
            agents, List.copyOf(transactionBuffer), trustGraphService.getTrustEdges(), metadata);

        Map<String, Object> result = new LinkedHashMap<>(metadata);
        result.put("dataset_files", dataset.keySet());
        result.put("dataset", dataset);

        return result;
    }

    private void generatePopulation(int popSize, FraudScenario scenario) {
        double fraudRate = scenario.fraudRate();
        int fraudCount = (int)(popSize * fraudRate);

        for (int i = 0; i < popSize - fraudCount; i++) {
            AgentProfile profile = AgentProfile.random(random);
            agents.add(Agent.create(AgentType.NORMAL, profile));
        }

        if (scenario.enabledFraudTypes().contains("sybil")) {
            for (int i = 0; i < Math.max(1, fraudCount / 4); i++) {
                agents.add(Agent.create(AgentType.FRAUD_SYBIL));
            }
        }
        if (scenario.enabledFraudTypes().contains("collusion")) {
            for (int i = 0; i < Math.max(1, fraudCount / 4); i++) {
                agents.add(Agent.create(AgentType.FRAUD_COLLUSION));
            }
        }
        if (scenario.enabledFraudTypes().contains("replay")) {
            for (int i = 0; i < Math.max(1, fraudCount / 4); i++) {
                agents.add(Agent.create(AgentType.FRAUD_REPLAY));
            }
        }
        if (scenario.enabledFraudTypes().contains("dos")) {
            for (int i = 0; i < Math.max(1, fraudCount / 4); i++) {
                agents.add(Agent.create(AgentType.FRAUD_DOS));
            }
        }

        int remaining = popSize - agents.size();
        for (int i = 0; i < remaining; i++) {
            agents.add(Agent.create(AgentType.NORMAL, AgentProfile.random(random)));
        }

        Collections.shuffle(agents, random);
    }

    private void generateNormalTraffic() {
        Instant windowStart = Instant.now().minus(config.getTimeWindowHours(), java.time.temporal.ChronoUnit.HOURS);
        long windowSeconds = Duration.ofHours(config.getTimeWindowHours()).getSeconds();

        for (Agent agent : agents) {
            if (agent.type() != AgentType.NORMAL) continue;

            AgentProfile profile = agent.profile();
            Mandate mandate = Mandate.create(agent.agentId(), profile.getScope());
            agentMandates.computeIfAbsent(agent.agentId(), k -> new ArrayList<>()).add(mandate);

            double activityLevel = samplePowerLaw(0.5, 3.0);
            int txCount = Math.max(1, (int)(profile.getAvgTxPerHour() * config.getTimeWindowHours() * activityLevel));

            boolean burst = random.nextDouble() < profile.getBurstProbability();
            if (burst) txCount *= 3;

            for (int i = 0; i < txCount; i++) {
                Agent recipient = pickWeightedRecipient(agent.agentId());
                if (recipient == null) continue;

                double amount = sampleLogNormal(profile.getAvgAmount());
                amount = Math.min(amount, profile.getMaxAmount());
                Instant txTime = sampleTimeWithDiurnalPattern(windowStart, windowSeconds);

                transactionBuffer.add(Transaction.create(
                    mandate.mandateId(), agent.agentId(), recipient.agentId(),
                    BigDecimal.valueOf(amount).setScale(6, java.math.RoundingMode.HALF_UP),
                    false, "normal"
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
        return Math.max(0.001, Math.exp(random.nextGaussian() * sigma + mu));
    }

    private Instant sampleTimeWithDiurnalPattern(Instant windowStart, long windowSeconds) {
        double hourOfDay = random.nextDouble() * 24;
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

    private void injectFraud(FraudScenario scenario) {
        Set<String> types = scenario.enabledFraudTypes();

        List<Agent> sybilAgents = agents.stream().filter(a -> a.type() == AgentType.FRAUD_SYBIL).toList();
        List<Agent> collusionAgents = agents.stream().filter(a -> a.type() == AgentType.FRAUD_COLLUSION).toList();
        List<Agent> replayAgents = agents.stream().filter(a -> a.type() == AgentType.FRAUD_REPLAY).toList();
        List<Agent> dosAgents = agents.stream().filter(a -> a.type() == AgentType.FRAUD_DOS).toList();

        if (types.contains("sybil") && !sybilAgents.isEmpty())
            sybilClusterFraud.inject(sybilAgents, transactionBuffer);
        if (types.contains("collusion") && !collusionAgents.isEmpty())
            collusionRingFraud.inject(collusionAgents, transactionBuffer);
        if (types.contains("replay") && !replayAgents.isEmpty())
            mandateReplayFraud.inject(replayAgents, agentMandates, transactionBuffer);
        if (types.contains("dos") && !dosAgents.isEmpty())
            micropaymentDosFraud.inject(dosAgents, transactionBuffer);
        if (types.contains("velocity"))
            velocityAnomalyFraud.inject(agents, agentMandates, transactionBuffer);
    }

    private void updateReputation() {
        for (Transaction tx : transactionBuffer) {
            if (tx.isFraudLabel()) {
                reputationTracker.recordFraudFlag(tx.fromAgentId());
            } else {
                reputationTracker.recordSuccessfulPayment(tx.fromAgentId(), tx.amount().doubleValue());
            }
        }
    }

    private void persistTransactions() {
        String sql = "INSERT INTO transactions (tx_id, mandate_id, from_agent_id, to_agent_id, amount, currency, timestamp, is_fraud_label) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        for (Transaction tx : transactionBuffer) {
            jdbc.update(sql, tx.txId(), tx.mandateId(), tx.fromAgentId(), tx.toAgentId(),
                tx.amount(), tx.currency(), tx.timestamp(), tx.isFraudLabel());
        }
    }
}
