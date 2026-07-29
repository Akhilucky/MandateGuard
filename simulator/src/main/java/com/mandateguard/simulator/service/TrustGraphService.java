package com.mandateguard.simulator.service;

import com.mandateguard.simulator.model.Agent;
import com.mandateguard.simulator.model.TrustEdge;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class TrustGraphService {

    private final Queue<TrustEdge> trustBuffer = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Set<UUID>> trustAdjacency = new HashMap<>();

    public void initializeTrustGraph(List<Agent> agents, Random rng) {
        trustBuffer.clear();
        trustAdjacency.clear();

        for (Agent agent : agents) {
            int trustCount = 2 + rng.nextInt(5);
            List<Agent> candidates = agents.stream()
                .filter(a -> !a.agentId().equals(agent.agentId()))
                .toList();

            for (int i = 0; i < Math.min(trustCount, candidates.size()); i++) {
                Agent target = candidates.get(rng.nextInt(candidates.size()));
                double trustScore = 0.3 + rng.nextDouble() * 0.7;
                TrustEdge edge = TrustEdge.create(agent.agentId(), target.agentId(), trustScore);
                trustBuffer.add(edge);
                trustAdjacency.computeIfAbsent(agent.agentId(), k -> new HashSet<>()).add(target.agentId());
            }
        }
    }

    public void recordTrustChange(UUID from, UUID to, double delta) {
        TrustEdge updated = new TrustEdge(
            UUID.randomUUID(), from, to,
            Math.max(0, Math.min(1, 0.5 + delta)),
            java.time.Instant.now(), true
        );
        trustBuffer.add(updated);
    }

    public List<TrustEdge> getTrustEdges() {
        return List.copyOf(trustBuffer);
    }

    public Set<UUID> getTrustedBy(UUID agentId) {
        return trustAdjacency.getOrDefault(agentId, Set.of());
    }

    public boolean trusts(UUID from, UUID to) {
        return trustAdjacency.getOrDefault(from, Set.of()).contains(to);
    }
}
