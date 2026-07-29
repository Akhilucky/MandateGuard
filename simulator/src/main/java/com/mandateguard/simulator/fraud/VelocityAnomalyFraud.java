package com.mandateguard.simulator.fraud;

import com.mandateguard.simulator.model.Agent;
import com.mandateguard.simulator.model.Mandate;
import com.mandateguard.simulator.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class VelocityAnomalyFraud {

    public void inject(List<Agent> allAgents, Map<UUID, List<Mandate>> agentMandates,
                       Queue<Transaction> buffer) {
        int count = Math.max(1, allAgents.size() / 20);
        List<Agent> normalAgents = allAgents.stream()
            .filter(a -> a.type() == Agent.AgentType.NORMAL)
            .limit(count).toList();

        for (Agent agent : normalAgents) {
            Mandate mandate = Mandate.create(agent.agentId());
            agentMandates.computeIfAbsent(agent.agentId(), k -> new ArrayList<>()).add(mandate);

            for (int i = 0; i < 30 + (int)(Math.random() * 50); i++) {
                buffer.add(Transaction.create(
                    mandate.mandateId(), agent.agentId(), UUID.randomUUID(),
                    BigDecimal.valueOf(50.0 + Math.random() * 200.0).setScale(6, RoundingMode.HALF_UP),
                    true
                ));
            }
        }
    }
}
