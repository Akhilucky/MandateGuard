package com.mandateguard.ledger.controller;

import com.mandateguard.ledger.model.TransactionRecord;
import com.mandateguard.ledger.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ledger")
public class LedgerController {

    private final TransactionRepository repository;

    public LedgerController(TransactionRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionRecord> ingest(@RequestBody TransactionRecord tx) {
        repository.save(tx);
        return ResponseEntity.ok(tx);
    }

    @GetMapping("/transactions/{txId}")
    public ResponseEntity<TransactionRecord> getTransaction(@PathVariable UUID txId) {
        return repository.findById(txId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/transactions/agent/{agentId}")
    public List<TransactionRecord> getAgentTransactions(
            @PathVariable UUID agentId,
            @RequestParam(defaultValue = "100") int limit) {
        return repository.findByAgent(agentId, limit);
    }

    @GetMapping("/transactions/recent")
    public List<TransactionRecord> getRecent(@RequestParam(defaultValue = "50") int limit) {
        return repository.findRecent(limit);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of("totalTransactions", repository.count());
    }
}
