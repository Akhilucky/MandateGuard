package com.mandateguard.simulator.fraud;

import com.mandateguard.simulator.model.Agent;
import com.mandateguard.simulator.model.Mandate;
import com.mandateguard.simulator.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class CollusionRingFraud {

    public void inject(List<Agent> collusionAgents, Queue<Transaction> buffer) {
        if (collusionAgents.size() < 3) return;

        List<Agent> ring = collusionAgents.subList(0, Math.min(collusionAgents.size(), 6));
        BigDecimal amount = BigDecimal.valueOf(1.0 + Math.random() * 5.0).setScale(6, RoundingMode.HALF_UP);

        for (int i = 0; i < ring.size(); i++) {
            Agent from = ring.get(i);
            Agent to = ring.get((i + 1) % ring.size());
            Mandate mandate = Mandate.create(from.agentId());

            for (int j = 0; j < 3; j++) {
                buffer.add(Transaction.create(
                    mandate.mandateId(), from.agentId(), to.agentId(), amount, true
                ));
            }
        }
    }
}
