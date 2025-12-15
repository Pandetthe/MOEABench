package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "experiment_part_solution")
public class ExperimentPartSolutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "experiment_part_id", nullable = false)
    private ExperimentPart experimentPart;

    @Column(nullable = false)
    private int iteration;

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

    protected ExperimentPartSolutionEntity() {}

    public ExperimentPartSolutionEntity(
            ExperimentPart part,
            int iteration,
            Map<String, Double> objectives,
            Map<String, Double> constraints
    ) {
        this.experimentPart = part;
        this.iteration = iteration;
        this.objectives = objectives;
        this.constraints = constraints;
    }

    public int getIteration() { return iteration; }
    public Map<String, Double> getObjectives() { return objectives; }
    public Map<String, Double> getConstraints() { return constraints; }
}
