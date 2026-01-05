package pl.edu.agh.to.kotospring.server.services.interfaces;

import pl.edu.agh.to.kotospring.server.models.QueueData;

public interface ExperimentExecutionService {
    void enqueue(QueueData queueData);
}
