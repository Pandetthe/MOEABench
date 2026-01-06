package pl.edu.agh.to.kotospring.server.services.interfaces;

import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.server.entities.ExperimentFull;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.util.List;
import java.util.Optional;

public interface ExperimentService {
    ExperimentFull createExperimentFull(CreateExperimentRequest request, int runsNo);
    Experiment createExperiment(CreateExperimentRequest request);
    List<Experiment> getExperiments();
    Optional<ExperimentFull> getExperiment(long id);
    Optional<ExperimentFull> getExperimentStatus(long id);
    Optional<ExperimentPart> getExperimentPart(long experimentId, long partId);
    Optional<Experiment> getExperimentResult(long id);
    Optional<ExperimentPart> getExperimentPartResult(long id, long partId);
    boolean deleteExperimentFull(long id);
    boolean deleteExperiment(long id);
}
