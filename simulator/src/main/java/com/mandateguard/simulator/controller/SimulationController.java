package com.mandateguard.simulator.controller;

import com.mandateguard.simulator.service.SimulationEngine;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/simulate")
public class SimulationController {

    private final SimulationEngine engine;

    public SimulationController(SimulationEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/run")
    public Map<String, Object> runSimulation(
            @RequestParam(required = false) String scenario,
            @RequestParam(required = false) Integer population) {
        return engine.runSimulation(scenario, population);
    }

    @PostMapping("/run/{scenario}")
    public Map<String, Object> runScenario(@PathVariable String scenario,
                                            @RequestParam(required = false) Integer population) {
        return engine.runSimulation(scenario, population);
    }

    @GetMapping("/scenarios")
    public Map<String, Object> getScenarios() {
        return Map.of(
            "scenarios", Map.of(
                "A", Map.of("name", "Replay Only", "description", "Only mandate replay attacks", "fraud_rate", 0.02),
                "B", Map.of("name", "Replay + Sybil", "description", "Replay and sybil cluster attacks", "fraud_rate", 0.03),
                "C", Map.of("name", "Full Spectrum", "description", "All fraud types active", "fraud_rate", 0.03),
                "D", Map.of("name", "Targeted", "description", "High-rate attack on specific profiles", "fraud_rate", 0.05)
            )
        );
    }
}
