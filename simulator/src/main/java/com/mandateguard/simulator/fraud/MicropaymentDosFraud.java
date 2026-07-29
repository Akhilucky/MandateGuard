package com.mandateguard.simulator.fraud;

import com.mandateguard.simulator.model.Agent;
import com.mandateguard.simulator.model.Mandate;
import com.mandateguard.simulator.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class MicropaymentDosFraud {

    public void inject(List<Agent> dosAgents, Queue<Transaction> buffer) {
        for (Agent agent : dosAgents) {
            Mandate mandate = Mandate.create(agent.agentId());
            UUID target = UUID.randomUUID();

            for (int i = 0; i < 100 + (int)(Math.random() * 400); i++) {
                buffer.add(Transaction.create(
                    mandate.mandateId(), agent.agentId(), target,
                    BigDecimal.valueOf(0.001).setScale(6, RoundingMode.HALF_UP),
                    true
                ));
            }
        }
    }
}
