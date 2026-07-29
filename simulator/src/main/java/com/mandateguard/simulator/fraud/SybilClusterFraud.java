package com.mandateguard.simulator.fraud;

import com.mandateguard.simulator.model.Agent;
import com.mandateguard.simulator.model.Mandate;
import com.mandateguard.simulator.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class SybilClusterFraud {

    public void inject(List<Agent> sybilAgents, Queue<Transaction> buffer) {
        if (sybilAgents.size() < 2) return;

        UUID cashOutTarget = UUID.randomUUID();

        for (int i = 0; i < sybilAgents.size() - 1; i++) {
            Agent a = sybilAgents.get(i);
            Agent b = sybilAgents.get(i + 1);
            Mandate mandate = Mandate.create(a.agentId());

            buffer.add(Transaction.create(
                mandate.mandateId(), a.agentId(), b.agentId(),
                BigDecimal.valueOf(0.1 + Math.random() * 0.5).setScale(6, RoundingMode.HALF_UP),
                true
            ));
        }

        Agent lastAgent = sybilAgents.get(sybilAgents.size() - 1);
        Mandate cashOutMandate = Mandate.create(lastAgent.agentId());
        buffer.add(Transaction.create(
            cashOutMandate.mandateId(), lastAgent.agentId(), cashOutTarget,
            BigDecimal.valueOf(10.0 + Math.random() * 50.0).setScale(6, RoundingMode.HALF_UP),
            true
        ));
    }
}
