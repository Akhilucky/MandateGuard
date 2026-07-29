package com.mandateguard.simulator.fraud;

import com.mandateguard.simulator.model.Agent;
import com.mandateguard.simulator.model.Mandate;
import com.mandateguard.simulator.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Component
public class MandateReplayFraud {

    public void inject(List<Agent> replayAgents, Map<UUID, List<Mandate>> agentMandates,
                       Queue<Transaction> buffer) {
        for (Agent agent : replayAgents) {
            Mandate mandate = Mandate.create(agent.agentId());
            agentMandates.computeIfAbsent(agent.agentId(), k -> new ArrayList<>()).add(mandate);

            UUID target = UUID.randomUUID();

            for (int i = 0; i < 10 + (int)(Math.random() * 20); i++) {
                buffer.add(Transaction.create(
                    mandate.mandateId(), agent.agentId(), target,
                    BigDecimal.valueOf(0.01 + Math.random() * 5.0).setScale(6, RoundingMode.HALF_UP),
                    true
                ));
            }
        }
    }
}
