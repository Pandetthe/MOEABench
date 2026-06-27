package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "experiment_part")
public class ExperimentPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Column(name = "problem", nullable = false)
    private String problem;

    @Column(name = "algorithm", nullable = false)
    private String algorithm;

    @Column(name = "budget", nullable = false)
    private int budget;

    @OneToMany(mappedBy = "experimentPart", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<ExperimentPartAlgorithmParameter> parameters = new HashSet<>();

    @OneToMany(mappedBy = "experimentPart", cascade = CascadeType.REMOVE)
    private final Set<ExperimentPartExecution> executions = new HashSet<>();

    public ExperimentPart() {
    }

    public ExperimentPart(String problem, String algorithm, int budget) {
        this.problem = Objects.requireNonNull(problem);
        this.algorithm = Objects.requireNonNull(algorithm);
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
        if (experiment != null && !experiment.getParts().contains(this)) {
            experiment.addPart(this);
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

    public int getBudget() {
        return budget;
    }

    public void setBudget(int budget) {
        this.budget = budget;
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
        if (parameter == null)
            return;
        if (this.parameters.remove(parameter)) {
            parameter.setExperimentPart(null);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExperimentPart that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
