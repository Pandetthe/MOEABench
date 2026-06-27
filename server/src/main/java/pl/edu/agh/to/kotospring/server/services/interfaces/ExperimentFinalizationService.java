package pl.edu.agh.to.kotospring.server.services.interfaces;

import pl.edu.agh.to.kotospring.server.entities.embeddables.RunId;

public interface ExperimentFinalizationService {
    void finalizeRunIfComplete(RunId runId);
}
