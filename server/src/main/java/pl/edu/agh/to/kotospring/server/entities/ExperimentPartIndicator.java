package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "experiment_part_indicator", uniqueConstraints = @UniqueConstraint(columnNames = { "experiment_part_id",
        "name" }))
public class ExperimentPartIndicator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "experiment_part_id", nullable = false)
    private ExperimentPartExecution experimentPartExecution;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "value")
    private Double value;

    public ExperimentPartIndicator() {
    }

    public ExperimentPartIndicator(String name, Double value) {
        this.name = name;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public ExperimentPartExecution getExperimentPartExecution() {
        return experimentPartExecution;
    }

    public void setExperimentPartExecution(ExperimentPartExecution experimentPartExecution) {
        this.experimentPartExecution = experimentPartExecution;
        if (experimentPartExecution != null && !experimentPartExecution.getIndicators().contains(this)) {
            experimentPartExecution.addIndicator(this);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExperimentPartIndicator that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
