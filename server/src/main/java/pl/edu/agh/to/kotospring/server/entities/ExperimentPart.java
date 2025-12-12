package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.ExperimentPartStatus;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
public class ExperimentPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @OneToMany(mappedBy = "experimentPart", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ExperimentPartAlgorithmParameter> parameters;

    @Column(nullable = false, length = 255)
    private String problem;

    @Column(nullable = false, length = 255)
    private String algorithm;

    @ElementCollection
    @CollectionTable(
            name = "experiment_part_indicators",
            joinColumns = @JoinColumn(name = "experiment_part_id")
    )
    @Column(name = "indicator")
    private Set<String> indicators;

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

    public ExperimentPart() {
        parameters = new ArrayList<>();
    }

    public ExperimentPart(String problem, String algorithm,
                          List<ExperimentPartAlgorithmParameter> parameters,
                          Set<String> indicators, int budget) {
        this.status = ExperimentPartStatus.QUEUED;
        this.problem = problem;
        this.algorithm = algorithm;
        this.parameters = parameters;
        this.indicators = indicators;
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

    public Set<String> getIndicators() {
        return indicators;
    }

    public void setIndicators(Set<String> indicators) {
        this.indicators = indicators;
    }

    public int getBudget() {
        return budget;
    }

    public void setBudget(int budget) {
        this.budget = budget;
    }

    public ExperimentPartStatus getStatus() {
        return status;
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
    public List<ExperimentPartAlgorithmParameter> getParameters() {
        return parameters;
    }
}
