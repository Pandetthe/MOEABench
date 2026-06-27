package pl.edu.agh.to.kotospring.server.services.implementation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartExecution;
import pl.edu.agh.to.kotospring.server.entities.ExperimentRun;
import pl.edu.agh.to.kotospring.server.entities.embeddables.RunId;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartExecutionRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRunRepository;
import pl.edu.agh.to.kotospring.server.services.interfaces.ExperimentFinalizationService;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class ExperimentFinalizationServiceImpl implements ExperimentFinalizationService {
    private final Logger logger = LoggerFactory.getLogger(ExperimentFinalizationServiceImpl.class);

    private final ExperimentRepository experimentRepository;
    private final ExperimentRunRepository experimentRunRepository;
    private final ExperimentPartExecutionRepository experimentPartExecutionRepository;
    private final EntityManager entityManager;

    public ExperimentFinalizationServiceImpl(
            ExperimentRepository experimentRepository,
            ExperimentRunRepository experimentRunRepository,
            ExperimentPartExecutionRepository experimentPartExecutionRepository,
            EntityManager entityManager) {
        this.experimentRepository = experimentRepository;
        this.experimentRunRepository = experimentRunRepository;
        this.experimentPartExecutionRepository = experimentPartExecutionRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeRunIfComplete(RunId runId) {
        Experiment experiment = entityManager.find(Experiment.class, runId.getExperimentId(),
                LockModeType.PESSIMISTIC_WRITE);
        ExperimentRun run = entityManager.find(ExperimentRun.class, runId, LockModeType.PESSIMISTIC_WRITE);
        List<ExperimentPartExecution> parts = experimentPartExecutionRepository.findAllByExperimentRunId(runId);

        boolean isStillRunning = parts.stream().anyMatch(
                p -> p.getStatus() == ExperimentPartStatus.RUNNING || p.getStatus() == ExperimentPartStatus.QUEUED);
        if (isStillRunning) {
            return;
        }

        long totalCount = parts.size();
        long completedCount = parts.stream().filter(p -> p.getStatus() == ExperimentPartStatus.COMPLETED).count();
        OffsetDateTime latestFinished = parts.stream()
                .map(ExperimentPartExecution::getFinishedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(OffsetDateTime.now());

        ExperimentRunStatus finalRunStatus = determineRunFinalStatus(totalCount, completedCount);
        run.setStatus(finalRunStatus);
        run.setFinishedAt(latestFinished);
        experimentRunRepository.saveAndFlush(run);
        logger.info("Run {} of experiment {} finished with status {}", runId.getRunNo(), runId.getExperimentId(),
                finalRunStatus);

        finalizeExperimentIfComplete(experiment);
    }

    private void finalizeExperimentIfComplete(Experiment experiment) {
        List<ExperimentRun> runs = experimentRunRepository.findAllByIdExperimentId(experiment.getId());
        boolean isStillRunning = runs.stream().anyMatch(
                r -> r.getStatus() == ExperimentRunStatus.IN_PROGRESS || r.getStatus() == ExperimentRunStatus.QUEUED);
        if (isStillRunning) {
            return;
        }

        long totalCount = runs.size();
        long successfulCount = runs.stream().filter(r -> r.getStatus() == ExperimentRunStatus.SUCCESS).count();
        long partialCount = runs.stream().filter(r -> r.getStatus() == ExperimentRunStatus.PARTIAL_SUCCESS).count();
        OffsetDateTime latestFinished = runs.stream()
                .map(ExperimentRun::getFinishedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(OffsetDateTime.now());

        ExperimentStatus finalStatus = determineExperimentFinalStatus(totalCount, successfulCount, partialCount);
        experiment.setStatus(finalStatus);
        experiment.setFinishedAt(latestFinished);
        experimentRepository.saveAndFlush(experiment);
        logger.info("Experiment {} fully finalized with status {}", experiment.getId(), finalStatus);
    }

    private ExperimentRunStatus determineRunFinalStatus(long totalCount, long completedCount) {
        if (completedCount == totalCount) {
            return ExperimentRunStatus.SUCCESS;
        } else if (completedCount > 0) {
            return ExperimentRunStatus.PARTIAL_SUCCESS;
        } else {
            return ExperimentRunStatus.FAILED;
        }
    }

    private ExperimentStatus determineExperimentFinalStatus(long totalCount, long successfulCount, long partialCount) {
        if (successfulCount == totalCount) {
            return ExperimentStatus.SUCCESS;
        } else if (successfulCount > 0 || partialCount > 0) {
            return ExperimentStatus.PARTIAL_SUCCESS;
        } else {
            return ExperimentStatus.FAILED;
        }
    }
}
