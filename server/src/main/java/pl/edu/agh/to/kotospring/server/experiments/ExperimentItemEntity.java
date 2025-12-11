package pl.edu.agh.to.kotospring.server.experiments;

import jakarta.persistence.*;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.ExperimentStatus;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
public class ExperimentItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private ExperimentEntity experiment;

    @OneToMany(mappedBy = "experimentItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExperimentItemAlgorithmParameterEntity> parameters;

    @Column(nullable = false, length = 255)
    private String problem;

    @Column(nullable = false, length = 255)
    private String algorithm;

    @ElementCollection
    @CollectionTable(
            name = "experiment_item_indicators",
            joinColumns = @JoinColumn(name = "experiment_item_id")
    )
    @Column(name = "indicator")
    private List<String> indicators;

    @Column(nullable = false)
    private int budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperimentStatus status;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    public ExperimentItemEntity() {}
    public Long getId() {
        return id;
    }

    public ExperimentEntity getExperiment() {
        return experiment;
    }

    public void setExperiment(ExperimentEntity experiment) {
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

    public List<String> getIndicators() {
        return indicators;
    }

    public void setIndicators(List<String> indicators) {
        this.indicators = indicators;
    }

    public int getBudget() {
        return budget;
    }

    public void setBudget(int budget) {
        this.budget = budget;
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(ExperimentStatus status) {
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
    public List<ExperimentItemAlgorithmParameterEntity> getParameters() {
        return parameters;
    }
    public void setParameters(List<ExperimentItemAlgorithmParameterEntity> parameters) {
        this.parameters = parameters;
    }
}
