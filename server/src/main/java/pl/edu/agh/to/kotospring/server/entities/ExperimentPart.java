package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
public class ExperimentPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @OneToMany(mappedBy = "experimentPart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final Set<ExperimentPartAlgorithmParameter> parameters = new HashSet<>();

    @Column(nullable = false, length = 255)
    private String problem;

    @Column(nullable = false, length = 255)
    private String algorithm;

    @OneToMany(mappedBy = "experimentPart", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExperimentPartIndicator> indicators = new HashSet<>();

    @Column(nullable = false)
    private int budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperimentPartStatus status;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @OneToMany(mappedBy = "experimentPart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ExperimentPartSolution> solutions = new HashSet<>();


    public ExperimentPart() {
        this.status = ExperimentPartStatus.QUEUED;
    }

    public ExperimentPart(String problem, String algorithm,
                          Collection<ExperimentPartAlgorithmParameter> parameters, int budget) {
        this();
        this.problem = Objects.requireNonNull(problem);
        this.algorithm = Objects.requireNonNull(algorithm);
        if (parameters != null) {
            this.parameters.addAll(parameters);
            this.parameters.forEach(p -> p.setExperimentPart(this));
        }
        this.budget = budget;
    }

    public Long getId() {
        return id;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
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
        indicator.setExperimentPart(this);
        this.indicators.add(indicator);
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

    public Set<ExperimentPartAlgorithmParameter> getParameters() {
        return Collections.unmodifiableSet(this.parameters);
    }

    public Set<ExperimentPartSolution> getSolutions() {
        return Collections.unmodifiableSet(solutions);
    }

    public void addSolution(ExperimentPartSolution solution) {
        if (solution == null) {
            throw new IllegalArgumentException("solution must not be null");
        }
        this.solutions.add(solution);
    }
}
