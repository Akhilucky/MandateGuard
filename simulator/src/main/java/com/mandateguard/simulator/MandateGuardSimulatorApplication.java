package com.mandateguard.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MandateGuardSimulatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(MandateGuardSimulatorApplication.class, args);
    }
}
