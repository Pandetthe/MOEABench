package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "experiment_part_solution")
public class ExperimentPartSolution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "experiment_part_id", nullable = false)
    private ExperimentPart experimentPart;

    @ElementCollection
    @CollectionTable(
            name = "experiment_part_solution_variable",
            joinColumns = @JoinColumn(name = "solution_id")
    )
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    private Map<String, String> variables = new HashMap<>();

    @ElementCollection
    @CollectionTable(
            name = "experiment_part_solution_objective",
            joinColumns = @JoinColumn(name = "solution_id")
    )
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    private Map<String, Double> objectives = new HashMap<>();

    @ElementCollection
    @CollectionTable(
            name = "experiment_part_solution_constraint",
            joinColumns = @JoinColumn(name = "solution_id")
    )
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    private Map<String, Double> constraints = new HashMap<>();

    protected ExperimentPartSolution() {}
    public ExperimentPartSolution(
            Map<String, String> variables,
            Map<String, Double> objectives,
            Map<String, Double> constraints
    ) {
        this.variables = variables;
        this.objectives = objectives;
        this.constraints = constraints;
    }


    public Long getId() {
        return id;
    }

    public ExperimentPart getExperimentPart() {
        return experimentPart;
    }
    public void setExperimentPart(ExperimentPart experimentPart) {
        this.experimentPart = experimentPart;
        if (experimentPart != null && !experimentPart.getSolutions().contains(this)) {
            experimentPart.addSolution(this);
        }
    }

    public Map<String, String> getVariables() { return Collections.unmodifiableMap(variables); }
    public void addVariable(String name, String value) {
        variables.put(name, value);
    }
    public void removeVariable(String name) {
        variables.remove(name);
    }

    public Map<String, Double> getObjectives() { return Collections.unmodifiableMap(objectives); }
    public void addObjective(String name, Double value) {
        objectives.put(name, value);
    }
    public void removeObjective(String name) {
        objectives.remove(name);
    }

    public Map<String, Double> getConstraints() { return Collections.unmodifiableMap(constraints); }
    public void addConstraint(String name, Double value) {
        constraints.put(name, value);
    }
    public void removeConstraint(String name) {
        constraints.remove(name);
    }
}
