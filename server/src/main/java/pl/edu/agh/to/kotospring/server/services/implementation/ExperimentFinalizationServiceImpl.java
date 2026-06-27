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
import java.util.List;

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

        long totalCount = 0;
        long completedCount = 0;
        OffsetDateTime latestFinished = null;
        for (ExperimentPartExecution p : parts) {
            totalCount++;
            ExperimentPartStatus s = p.getStatus();
            if (s == ExperimentPartStatus.RUNNING || s == ExperimentPartStatus.QUEUED) {
                return;
            }
            if (s == ExperimentPartStatus.COMPLETED) completedCount++;
            if (p.getFinishedAt() != null && (latestFinished == null || p.getFinishedAt().isAfter(latestFinished))) {
                latestFinished = p.getFinishedAt();
            }
        }
        if (latestFinished == null) latestFinished = OffsetDateTime.now();

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
        long totalCount = 0;
        long successfulCount = 0;
        long partialCount = 0;
        OffsetDateTime latestFinished = null;
        for (ExperimentRun r : runs) {
            totalCount++;
            ExperimentRunStatus s = r.getStatus();
            if (s == ExperimentRunStatus.IN_PROGRESS || s == ExperimentRunStatus.QUEUED) {
                return;
            }
            if (s == ExperimentRunStatus.SUCCESS) successfulCount++;
            else if (s == ExperimentRunStatus.PARTIAL_SUCCESS) partialCount++;
            if (r.getFinishedAt() != null && (latestFinished == null || r.getFinishedAt().isAfter(latestFinished))) {
                latestFinished = r.getFinishedAt();
            }
        }
        if (latestFinished == null) latestFinished = OffsetDateTime.now();

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
