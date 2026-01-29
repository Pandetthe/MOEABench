package pl.edu.agh.to.kotospring.server.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pl.edu.agh.to.kotospring.server.entities.ExperimentRun;
import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.server.entities.ExperimentGroup;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartExecution;
import pl.edu.agh.to.kotospring.server.models.PartStatusInfo;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ExperimentService {
        Experiment createExperiment(CreateExperimentRequest request);

        List<Experiment> getExperiments(String algorithm, String problem, String indicator,
                        ExperimentStatus status, OffsetDateTime start, OffsetDateTime end);

        Optional<Experiment> getExperiment(long id, ExperimentRunStatus status);

        Page<ExperimentRun> getExperimentRuns(String algorithm, String problem, String indicator,
                        ExperimentRunStatus status, OffsetDateTime start, OffsetDateTime end,
                        Pageable pageable);

        Optional<ExperimentRun> getExperimentRun(
                        long id, long runNo, String algorithm, String problem,
                        String indicator, ExperimentPartStatus partStatus);

        Optional<ExperimentPartExecution> getExperimentPart(long experimentId, long runNo, long partId);

        Optional<ExperimentStatus> getExperimentStatus(long id);

        Optional<ExperimentRunStatus> getExperimentRunStatus(long id, long runNo);

        Optional<PartStatusInfo> getExperimentPartStatus(long id, long runNo, long partId);

        Optional<Experiment> getExperimentResult(long id);

        Optional<ExperimentRun> getExperimentRunResult(long id, long runNo);

        Optional<ExperimentPartExecution> getExperimentPartResult(long id, long runNo, long partId);

        boolean deleteExperiment(long id);

        boolean deleteExperimentRun(long id, long runNo);

        GetExperimentAggregateResponse getExperimentAggregate(long experimentId);

        GetExperimentAggregateResponse getExperimentGroupAggregate(long groupId);

        Optional<String> getExperimentPartCsv(long id, long runNo, long partId);

        ExperimentGroup createExperimentGroup(String name);

        List<ExperimentGroup> getExperimentGroups();

        Optional<ExperimentGroup> getExperimentGroup(long groupId);

        ExperimentGroup addRunToExperimentGroup(Long groupId, Long id, Long runNo);

        boolean deleteExperimentGroup(long groupId);

        ExperimentGroup deleteRunFromExperimentGroup(Long groupId, Long id, Long runNo);



}
