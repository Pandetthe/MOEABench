package pl.edu.agh.to.kotospring.server.services.interfaces;

import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.util.List;
import java.util.Optional;

public interface ExperimentService {
    Experiment createExperiment(CreateExperimentRequest request);
    List<Experiment> getExperiments();
    Optional<Experiment> getExperiment(long id);
    Optional<Experiment> getExperimentStatus(long id);
    Optional<ExperimentPart> getExperimentPart(long experimentId, long partId);
    Optional<Experiment> getExperimentResult(long id);
    Optional<ExperimentPart> getExperimentPartResult(long id, long partId);
    boolean deleteExperiment(long id);
}
