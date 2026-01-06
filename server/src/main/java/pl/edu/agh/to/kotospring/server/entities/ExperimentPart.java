package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "experiment_part")
public class ExperimentPart {

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

    @OneToMany(mappedBy = "experimentPart", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<ExperimentPartAlgorithmParameter> parameters = new HashSet<>();

    @Column(name = "problem", nullable = false)
    private String problem;

    @Column(name = "algorithm", nullable = false)
    private String algorithm;

    @OneToMany(mappedBy = "experimentPart", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<ExperimentPartIndicator> indicators = new HashSet<>();

    @Column(name = "budget", nullable = false)
    private int budget;

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

    public ExperimentPart() {
        this.status = ExperimentPartStatus.QUEUED;
    }
    public ExperimentPart(String problem, String algorithm, int budget) {
        this();
        this.problem = Objects.requireNonNull(problem);
        this.algorithm = Objects.requireNonNull(algorithm);
        this.budget = budget;
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

    public Set<ExperimentPartAlgorithmParameter> getParameters() {
        return Collections.unmodifiableSet(this.parameters);
    }
    public void addParameter(ExperimentPartAlgorithmParameter parameter) {
        if (parameter == null) {
            throw new IllegalArgumentException("parameter must not be null");
        }
        if (this.parameters.add(parameter)) {
            parameter.setExperimentPart(this);
        }
    }
    public void removeParameter(ExperimentPartAlgorithmParameter parameter) {
        if (parameter == null) return;
        if (this.parameters.remove(parameter)) {
            parameter.setExperimentPart(null);
        }
    }

    public String getProblem() {
        return problem;
    }
    public void setProblem(String problem) {
        this.problem = problem;
    }

    public String getAlgorithm() {
        return algorithm;
    }
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
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
        if (indicator == null) return;
        if (this.indicators.remove(indicator)) {
            indicator.setExperimentPart(null);
        }
    }

    public int getBudget() {
        return this.budget;
    }
    public void setBudget(int budget) {
        this.budget = budget;
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
        if (solution == null) return;
        if (this.solutions.remove(solution)) {
            solution.setExperimentPart(null);
        }
    }
}
