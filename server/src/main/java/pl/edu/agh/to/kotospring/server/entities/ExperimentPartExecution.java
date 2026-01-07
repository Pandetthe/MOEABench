package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "experiment_part_execution")
public class ExperimentPartExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "experiment_id", referencedColumnName = "experiment_id", nullable = false),
            @JoinColumn(name = "run_no", referencedColumnName = "run_no", nullable = false)
    })
    private ExperimentRun experimentRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_part_id", nullable = false)
    private ExperimentPart experimentPart;

    @OneToMany(mappedBy = "experimentPart", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<ExperimentPartIndicator> indicators = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExperimentPartStatus status;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @OneToMany(mappedBy = "experimentPart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final Set<ExperimentPartSolution> solutions = new HashSet<>();

    public ExperimentPartExecution() {
        this.status = ExperimentPartStatus.QUEUED;
    }

    public ExperimentPartExecution(ExperimentPart experimentPart) {
        this();
        this.experimentPart = Objects.requireNonNull(experimentPart);
    }

    public Long getId() {
        return id;
    }

    public ExperimentRun getExperimentRun() {
        return experimentRun;
    }

    public void setExperimentRun(ExperimentRun experimentRun) {
        this.experimentRun = experimentRun;
        if (experimentRun != null && !experimentRun.getParts().contains(this)) {
            experimentRun.addPart(this);
        }
    }

    public ExperimentPart getExperimentPart() {
        return experimentPart;
    }

    public void setExperimentPart(ExperimentPart experimentPart) {
        this.experimentPart = experimentPart;
    }

    public String getProblem() {
        return experimentPart != null ? experimentPart.getProblem() : null;
    }

    public String getAlgorithm() {
        return experimentPart != null ? experimentPart.getAlgorithm() : null;
    }

    public int getBudget() {
        return experimentPart != null ? experimentPart.getBudget() : 0;
    }

    public Set<ExperimentPartAlgorithmParameter> getParameters() {
        return experimentPart != null ? experimentPart.getParameters() : Collections.emptySet();
    }

    public Set<ExperimentPartIndicator> getIndicators() {
        return Collections.unmodifiableSet(this.indicators);
    }

    public void addIndicator(ExperimentPartIndicator indicator) {
        if (indicator == null) {
            throw new IllegalArgumentException("indicator must not be null");
        }
        if (this.indicators.add(indicator)) {
            indicator.setExperimentPart(this);
        }
    }

    public void removeIndicator(ExperimentPartIndicator indicator) {
        if (indicator == null)
            return;
        if (this.indicators.remove(indicator)) {
            indicator.setExperimentPart(null);
        }
    }

    public ExperimentPartStatus getStatus() {
        return this.status;
    }

    public void setStatus(ExperimentPartStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Set<ExperimentPartSolution> getSolutions() {
        return Collections.unmodifiableSet(solutions);
    }

    public void addSolution(ExperimentPartSolution solution) {
        if (solution == null) {
            throw new IllegalArgumentException("solution must not be null");
        }
        if (this.solutions.add(solution)) {
            solution.setExperimentPart(this);
        }
    }

    public void removeSolution(ExperimentPartSolution solution) {
        if (solution == null)
            return;
        if (this.solutions.remove(solution)) {
            solution.setExperimentPart(null);
        }
    }
}
