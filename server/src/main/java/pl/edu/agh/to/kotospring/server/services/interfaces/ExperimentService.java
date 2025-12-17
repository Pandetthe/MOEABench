package pl.edu.agh.to.kotospring.server.services.interfaces;

import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.util.Optional;

public interface ExperimentService {
    CreateExperimentResponse createExperiment(CreateExperimentRequest request);
    GetExperimentsResponse getExperiments();
    Optional<GetExperimentResponse> getExperiment(long id);
    Optional<GetExperimentStatusResponse> getExperimentStatus(long id);
    Optional<GetExperimentPartStatusResponse> getExperimentStatus(long id, long partId);
    Optional<GetExperimentResultResponseData> getExperimentResult(long id);
    Optional<GetExperimentPartResultResponse> getExperimentResult(long id, long partId);
    boolean deleteExperiment(long id);
}
