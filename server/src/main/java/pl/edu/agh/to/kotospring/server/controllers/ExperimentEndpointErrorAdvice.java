package pl.edu.agh.to.kotospring.server.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartExecution;
import pl.edu.agh.to.kotospring.server.entities.embeddables.RunId;
import pl.edu.agh.to.kotospring.server.exceptions.NotFoundException;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartExecutionRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRunRepository;
import pl.edu.agh.to.kotospring.shared.experiments.ErrorResponse;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExperimentEndpointErrorAdvice {
    private static final Pattern FULL_PATH_PATTERN = Pattern.compile("^/experiments/(\\d+)/(\\d+)/(\\d+)(?:/.*)?$");

    private static final Pattern EXPERIMENT_PATH_PATTERN = Pattern.compile("^/experiments/(\\d+)(?:/.*)?$");

    private final ExperimentRepository experimentRepository;
    private final ExperimentRunRepository experimentRunRepository;
    private final ExperimentPartExecutionRepository experimentPartExecutionRepository;

    public ExperimentEndpointErrorAdvice(ExperimentRepository experimentRepository,
            ExperimentRunRepository experimentRunRepository,
            ExperimentPartExecutionRepository experimentPartExecutionRepository) {
        this.experimentRepository = experimentRepository;
        this.experimentRunRepository = experimentRunRepository;
        this.experimentPartExecutionRepository = experimentPartExecutionRepository;
    }

    @ExceptionHandler(NotFoundException.class)
    @Transactional(readOnly = true)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        String path = req.getRequestURI();

        Matcher fullMatcher = FULL_PATH_PATTERN.matcher(path);
        if (fullMatcher.matches()) {
            long expId = Long.parseLong(fullMatcher.group(1));
            long runNo = Long.parseLong(fullMatcher.group(2));
            long partId = Long.parseLong(fullMatcher.group(3));
            return translateDetailedNotFound(expId, runNo, partId);
        }

        Matcher expMatcher = EXPERIMENT_PATH_PATTERN.matcher(path);
        if (expMatcher.matches()) {
            long expId = Long.parseLong(expMatcher.group(1));
            if (!experimentRepository.existsById(expId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Experiment with id " + expId + " does not exist"));
            }
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    private ResponseEntity<ErrorResponse> translateDetailedNotFound(long expId, long runNo, long partId) {
        boolean expExists = experimentRepository.existsById(expId);
        RunId runId = new RunId(expId, runNo);
        boolean runExists = experimentRunRepository.existsById(runId);
        Optional<ExperimentPartExecution> partOpt = experimentPartExecutionRepository.findById(partId);

        if (!expExists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Experiment " + expId + " does not exist"));
        }

        if (!runExists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Experiment " + expId + " exists, but Run " + runNo + " does not"));
        }

        if (partOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Experiment part " + partId + " does not exist"));
        }

        ExperimentPartExecution part = partOpt.get();
        RunId actualRunId = part.getExperimentRun().getId();

        if (!actualRunId.equals(runId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(String.format("Part %d belongs to Experiment %d Run %d, not requested path",
                            partId, actualRunId.getExperimentId(), actualRunId.getRunNo())));
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Requested resource for Part " + partId + " was not found"));
    }
}