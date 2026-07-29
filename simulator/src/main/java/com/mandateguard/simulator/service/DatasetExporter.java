package com.mandateguard.simulator.service;

import com.mandateguard.simulator.model.Agent;
import com.mandateguard.simulator.model.Transaction;
import com.mandateguard.simulator.model.TrustEdge;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DatasetExporter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT;

    public Map<String, String> exportDataset(List<Agent> agents, List<Transaction> transactions,
                                              List<TrustEdge> trustEdges, Map<String, Object> metadata) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("agents.csv", exportAgents(agents));
        files.put("transactions.csv", exportTransactions(transactions));
        files.put("trust_graph.csv", exportTrustEdges(trustEdges));
        files.put("labels.csv", exportLabels(transactions));
        files.put("metadata.json", exportMetadata(metadata, agents.size(), transactions.size()));

        return files;
    }

    private String exportAgents(List<Agent> agents) {
        StringBuilder sb = new StringBuilder();
        sb.append("agent_id,type,profile,created_at,reputation_score,reputation_history\n");
        for (Agent a : agents) {
            sb.append(String.format("%s,%s,%s,%s,%.4f,%.4f\n",
                a.agentId(), a.type(), a.profile().getScope(),
                FMT.format(a.createdAt()), a.reputationScore(), a.reputationDelta()));
        }
        return sb.toString();
    }

    private String exportTransactions(List<Transaction> txs) {
        StringBuilder sb = new StringBuilder();
        sb.append("tx_id,mandate_id,from_agent_id,to_agent_id,amount,currency,timestamp,is_fraud\n");
        for (Transaction tx : txs) {
            sb.append(String.format("%s,%s,%s,%s,%.6f,%s,%s,%b\n",
                tx.txId(), tx.mandateId(), tx.fromAgentId(), tx.toAgentId(),
                tx.amount(), tx.currency(), FMT.format(tx.timestamp()), tx.isFraudLabel()));
        }
        return sb.toString();
    }

    private String exportTrustEdges(List<TrustEdge> edges) {
        StringBuilder sb = new StringBuilder();
        sb.append("trust_id,from_agent_id,to_agent_id,trust_score,established_at,active\n");
        for (TrustEdge e : edges) {
            sb.append(String.format("%s,%s,%s,%.4f,%s,%b\n",
                e.trustId(), e.fromAgentId(), e.toAgentId(),
                e.trustScore(), FMT.format(e.establishedAt()), e.active()));
        }
        return sb.toString();
    }

    private String exportLabels(List<Transaction> txs) {
        StringBuilder sb = new StringBuilder();
        sb.append("tx_id,is_fraud\n");
        for (Transaction tx : txs) {
            sb.append(String.format("%s,%b\n", tx.txId(), tx.isFraudLabel()));
        }
        return sb.toString();
    }

    private String exportMetadata(Map<String, Object> meta, int agentCount, int txCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(String.format("  \"agent_count\": %d,\n", agentCount));
        sb.append(String.format("  \"transaction_count\": %d,\n", txCount));
        for (Map.Entry<String, Object> e : meta.entrySet()) {
            sb.append(String.format("  \"%s\": \"%s\",\n", e.getKey(), e.getValue()));
        }
        sb.append("  \"benchmark_version\": \"1.0\"\n");
        sb.append("}\n");
        return sb.toString();
    }
}
