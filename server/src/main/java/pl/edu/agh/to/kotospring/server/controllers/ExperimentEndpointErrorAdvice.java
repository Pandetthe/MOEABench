package pl.edu.agh.to.kotospring.server.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;
import pl.edu.agh.to.kotospring.server.exceptions.NotFoundException;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartRepository;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExperimentEndpointErrorAdvice {

    private static final Pattern EXPERIMENT_ID =
            Pattern.compile("^/experiments/(\\d+)(?:/.*)?$");

    private static final Pattern EXPERIMENT_ID_AND_PART_ID =
            Pattern.compile("^/experiments/(\\d+)/(?:status|result)/(\\d+)$");

    private static final Pattern DELETE_EXPERIMENT =
            Pattern.compile("^/experiments/(\\d+)$");

    private final ExperimentRepository experimentRepository;
    private final ExperimentPartRepository experimentPartRepository;

    public ExperimentEndpointErrorAdvice(ExperimentRepository experimentRepository,
                                         ExperimentPartRepository experimentPartRepository) {
        this.experimentRepository = experimentRepository;
        this.experimentPartRepository = experimentPartRepository;
    }

    @ExceptionHandler(NotFoundException.class)
    @Transactional(readOnly = true)
    public ResponseEntity<String> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        String path = req.getRequestURI();

        Matcher m2 = EXPERIMENT_ID_AND_PART_ID.matcher(path);
        if (m2.matches()) {
            long id = Long.parseLong(m2.group(1));
            long partId = Long.parseLong(m2.group(2));

            return translateExperimentAndPart(path, id, partId);
        }

        Matcher m1 = EXPERIMENT_ID.matcher(path);
        if (m1.matches()) {
            long id = Long.parseLong(m1.group(1));

            if (DELETE_EXPERIMENT.matcher(path).matches()
                    && "DELETE".equalsIgnoreCase(req.getMethod())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Experiment with id " + id + " doesnt exist");
            }

            if (!experimentRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Experiment with id " + id + " doesnt exist");
            }


            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }


        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    private ResponseEntity<String> translateExperimentAndPart(String path, long id, long partId) {
        Optional<Experiment> expOpt = experimentRepository.findWithPartsById(id);
        boolean expExists = expOpt.isPresent();
        List<Long> availableParts = expOpt
                .map(this::extractPartIdsSorted)
                .orElse(List.of());

        Optional<Long> belongsTo = experimentPartRepository.findById(partId)
                .map(ExperimentPart::getExperiment)
                .map(Experiment::getId);

        if (expExists && belongsTo.isPresent()) {
            long x = belongsTo.get();
            if (x == id) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Unexpected: part exists and belongs to experiment");
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Experiment part with id " + partId + " doesnt belong to experiment with id " + id
                            + ", experiment parts available for experiment " + id + ": " + availableParts);
        }

        if (expExists && belongsTo.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Experiment part with id " + partId + " doesnt exist, experiment parts available for experiment "
                            + id + ": " + availableParts);
        }

        if (!expExists && belongsTo.isPresent()) {
            long x = belongsTo.get();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Experiment with id " + id + " doesnt exist, experiment part with id " + partId
                            + " belongs to experiment with id " + x);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Experiment and experiment part dont exist");
    }

    private List<Long> extractPartIdsSorted(Experiment exp) {
        if (exp.getParts() == null) return List.of();
        return exp.getParts().stream()
                .map(ExperimentPart::getId)
                .filter(x -> x != null)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
