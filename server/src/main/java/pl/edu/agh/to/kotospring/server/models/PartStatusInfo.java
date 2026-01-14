package pl.edu.agh.to.kotospring.server.models;

import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;

public record PartStatusInfo(ExperimentPartStatus status, String errorMessage) {}