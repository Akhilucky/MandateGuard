package com.mandateguard.simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "simulator")
public class SimulatorConfig {
    private int populationSize = 1000;
    private int timeWindowHours = 24;
    private double fraudRate = 0.02;
    private double avgTransactionsPerHour = 5;

    public int getPopulationSize() { return populationSize; }
    public void setPopulationSize(int populationSize) { this.populationSize = populationSize; }
    public int getTimeWindowHours() { return timeWindowHours; }
    public void setTimeWindowHours(int timeWindowHours) { this.timeWindowHours = timeWindowHours; }
    public double getFraudRate() { return fraudRate; }
    public void setFraudRate(double fraudRate) { this.fraudRate = fraudRate; }
    public double getAvgTransactionsPerHour() { return avgTransactionsPerHour; }
    public void setAvgTransactionsPerHour(double avgTransactionsPerHour) { this.avgTransactionsPerHour = avgTransactionsPerHour; }
}
