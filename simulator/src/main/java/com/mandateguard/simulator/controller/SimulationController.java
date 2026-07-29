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
    public Map<String, Object> runSimulation() {
        return engine.runSimulation();
    }
}
