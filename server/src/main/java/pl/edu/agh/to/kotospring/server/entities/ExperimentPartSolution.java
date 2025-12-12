package pl.edu.agh.to.kotospring.server.entities;

import java.util.Map;

public record ExperimentPartSolution(
        Map<String, Object> variables,
        Map<String, Double> objectives,
        Map<String, Double> constraints
) {}