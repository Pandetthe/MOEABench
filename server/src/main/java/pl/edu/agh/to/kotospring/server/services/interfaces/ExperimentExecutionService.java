package pl.edu.agh.to.kotospring.server.services.interfaces;

import pl.edu.agh.to.kotospring.server.models.QueueData;

public interface ExperimentExecutionService {
    void partStatusManager(QueueData queueData);
    void runExperimentPart(QueueData queueData);
}
