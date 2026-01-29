package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name= "experiment_group")
public class ExperimentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ElementCollection
    @CollectionTable(name = "experiment_group_problems", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "problem")
    private Set<String> problems = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "experiment_group_algorithms", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "algorithm")
    private Set<String> algorithms = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "experiment_group_runs",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = {
                    @JoinColumn(name = "experiment_id", referencedColumnName = "experiment_id"),
                    @JoinColumn(name = "run_no", referencedColumnName = "run_no")
            }
    )
    private final Set<ExperimentRun> runs = new HashSet<>();

    public ExperimentGroup() {
    }

    public ExperimentGroup(String name) {
        this.name = name;
    }

    public void addRun(ExperimentRun run) {
        if (run == null) {
            throw new IllegalArgumentException("Run must not be null");
        }

        if (!this.runs.isEmpty()) {
            Set<String> currentRunProblems = new HashSet<>();
            Set<String> currentRunAlgorithms = new HashSet<>();
            for (ExperimentPartExecution execution : run.getParts()) {
                ExperimentPart partDefinition = execution.getExperimentPart();
                if (partDefinition != null) {
                    currentRunProblems.add(partDefinition.getProblem());
                    currentRunAlgorithms.add(partDefinition.getAlgorithm());
                }
            }

            if (!currentRunProblems.equals(this.problems) || !currentRunAlgorithms.equals(this.algorithms)) {
                throw new IllegalArgumentException("Run is not compatible with other runs in the group");
            }
        }


        boolean isAdded = this.runs.add(run);
        if (isAdded && runs.size() == 1) {
            for (ExperimentPartExecution execution : run.getParts()) {
                ExperimentPart partDefinition = execution.getExperimentPart();
                if (partDefinition != null) {
                    this.problems.add(partDefinition.getProblem());
                    this.algorithms.add(partDefinition.getAlgorithm());
                }

            }
        }
    }

    public boolean removeRun(ExperimentRun run) {
        if (run == null)
            return false;
        boolean isRemoved = this.runs.remove(run);
        if (isRemoved && this.runs.isEmpty()) {
            this.problems.clear();
            this.algorithms.clear();
        }
        return isRemoved;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<ExperimentRun> getRuns() {
        return runs;
    }

    public Set<String> getProblems() {
        return problems;
    }

    public Set<String> getAlgorithms() {
        return algorithms;
    }
}
