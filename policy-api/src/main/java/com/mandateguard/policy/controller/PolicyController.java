package com.mandateguard.policy.controller;

import com.mandateguard.policy.model.Alert;
import com.mandateguard.policy.model.MandateHold;
import com.mandateguard.policy.repository.AlertRepository;
import com.mandateguard.policy.repository.HoldRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PolicyController {

    private final AlertRepository alertRepository;
    private final HoldRepository holdRepository;

    public PolicyController(AlertRepository alertRepository, HoldRepository holdRepository) {
        this.alertRepository = alertRepository;
        this.holdRepository = holdRepository;
    }

    @PostMapping("/alerts")
    public ResponseEntity<Alert> createAlert(@RequestBody Map<String, Object> body) {
        UUID txId = UUID.fromString((String) body.get("tx_id"));
        UUID agentId = UUID.fromString((String) body.get("agent_id"));
        double riskScore = ((Number) body.get("risk_score")).doubleValue();
        String reason = (String) body.get("reason");

        Alert alert = Alert.create(txId, agentId, riskScore, reason);
        alertRepository.save(alert);
        return ResponseEntity.ok(alert);
    }

    @GetMapping("/alerts")
    public List<Alert> getAlerts(@RequestParam(defaultValue = "50") int limit) {
        return alertRepository.findAll(limit);
    }

    @GetMapping("/alerts/unresolved")
    public List<Alert> getUnresolvedAlerts() {
        return alertRepository.findUnresolved();
    }

    @PutMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<Map<String, String>> resolveAlert(@PathVariable UUID alertId) {
        alertRepository.resolve(alertId);
        return ResponseEntity.ok(Map.of("status", "resolved"));
    }

    @PostMapping("/mandates/{mandateId}/hold")
    public ResponseEntity<MandateHold> holdMandate(
            @PathVariable UUID mandateId,
            @RequestBody Map<String, String> body) {
        UUID agentId = UUID.fromString(body.get("agent_id"));
        String reason = body.getOrDefault("reason", "manual hold");

        MandateHold hold = MandateHold.create(mandateId, agentId, reason);
        holdRepository.save(hold);
        return ResponseEntity.ok(hold);
    }

    @GetMapping("/mandates/holds")
    public List<MandateHold> getHolds() {
        return holdRepository.findAll();
    }
}
