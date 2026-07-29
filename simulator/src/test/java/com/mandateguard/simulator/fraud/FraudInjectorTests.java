package com.mandateguard.simulator.fraud;

import com.mandateguard.simulator.model.Agent;
import com.mandateguard.simulator.model.Mandate;
import com.mandateguard.simulator.model.Agent.AgentType;
import com.mandateguard.simulator.model.Transaction;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FraudInjectorTests {

    @Test
    void mandateReplayProducesMultipleTxsOnSameMandate() {
        MandateReplayFraud fraud = new MandateReplayFraud();
        List<Agent> agents = List.of(Agent.create(AgentType.FRAUD_REPLAY));
        Map<UUID, List<Mandate>> mandates = new HashMap<>();
        Queue<Transaction> buffer = new LinkedList<>();

        fraud.inject(agents, mandates, buffer);

        assertFalse(buffer.isEmpty());
        Set<UUID> mandateIds = new HashSet<>();
        for (Transaction tx : buffer) {
            mandateIds.add(tx.mandateId());
            assertTrue(tx.isFraudLabel());
        }
        assertTrue(mandateIds.size() <= 1, "All replay txs should reference the same mandate");
        assertTrue(buffer.size() >= 10, "Should produce at least 10 replay transactions");
    }

    @Test
    void micropaymentDosProducesHighFrequency() {
        MicropaymentDosFraud fraud = new MicropaymentDosFraud();
        List<Agent> agents = List.of(Agent.create(AgentType.FRAUD_DOS));
        Queue<Transaction> buffer = new LinkedList<>();

        fraud.inject(agents, buffer);

        assertTrue(buffer.size() >= 100, "DoS should produce at least 100 transactions");
        for (Transaction tx : buffer) {
            assertTrue(tx.isFraudLabel());
            assertTrue(tx.amount().doubleValue() < 0.01, "DoS amounts should be sub-cent");
        }
    }

    @Test
    void sybilClusterCreatesChainThenCashout() {
        SybilClusterFraud fraud = new SybilClusterFraud();
        List<Agent> agents = List.of(
            Agent.create(AgentType.FRAUD_SYBIL),
            Agent.create(AgentType.FRAUD_SYBIL),
            Agent.create(AgentType.FRAUD_SYBIL)
        );
        Queue<Transaction> buffer = new LinkedList<>();

        fraud.inject(agents, buffer);

        assertTrue(buffer.size() >= 3, "Should produce chain + cashout transactions");
        long fraudCount = buffer.stream().filter(Transaction::isFraudLabel).count();
        assertEquals(buffer.size(), fraudCount, "All sybil transactions should be labeled fraud");
    }

    @Test
    void collusionRingCreatesCycle() {
        CollusionRingFraud fraud = new CollusionRingFraud();
        List<Agent> agents = List.of(
            Agent.create(AgentType.FRAUD_COLLUSION),
            Agent.create(AgentType.FRAUD_COLLUSION),
            Agent.create(AgentType.FRAUD_COLLUSION)
        );
        Queue<Transaction> buffer = new LinkedList<>();

        fraud.inject(agents, buffer);

        assertTrue(buffer.size() >= 9, "Ring should produce cycle transactions");
        Set<UUID> fromAgents = new HashSet<>();
        for (Transaction tx : buffer) {
            fromAgents.add(tx.fromAgentId());
            assertTrue(tx.isFraudLabel());
        }
        assertEquals(3, fromAgents.size(), "All 3 ring members should send");
    }

    @Test
    void velocityAnomalyProducesSpikeTxs() {
        VelocityAnomalyFraud fraud = new VelocityAnomalyFraud();
        List<Agent> agents = new ArrayList<>();
        for (int i = 0; i < 50; i++) agents.add(Agent.create(AgentType.NORMAL));
        Map<UUID, List<Mandate>> mandates = new HashMap<>();
        Queue<Transaction> buffer = new LinkedList<>();

        fraud.inject(agents, mandates, buffer);

        assertFalse(buffer.isEmpty());
        for (Transaction tx : buffer) {
            assertTrue(tx.isFraudLabel());
            assertTrue(tx.amount().doubleValue() > 50.0, "Velocity anomaly amounts should be high");
        }
    }
}
