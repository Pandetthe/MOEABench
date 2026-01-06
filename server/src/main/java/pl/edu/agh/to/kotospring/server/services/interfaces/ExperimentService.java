package pl.edu.agh.to.kotospring.server.services.interfaces;

import pl.edu.agh.to.kotospring.server.entities.ExperimentRun;
import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.util.List;
import java.util.Optional;

public interface ExperimentService {
    Experiment createExperiment(CreateExperimentRequest request);
    List<Experiment> getExperiments();
    Optional<Experiment> getExperiment(long id);
    Optional<ExperimentRun> getExperimentRun(long id, long runNo);
    Optional<ExperimentPart> getExperimentPart(long experimentId, long runNo, long partId);

//    Optional<ExperimentRun> getExperimentResult(long id);
//    Optional<ExperimentPart> getExperimentPartResult(long id, long partId);
    boolean deleteExperiment(long id);
    boolean deleteExperimentRun(long id, long runNo);
}
