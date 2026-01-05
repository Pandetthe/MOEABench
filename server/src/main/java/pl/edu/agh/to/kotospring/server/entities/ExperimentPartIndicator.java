package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;
@Entity
@Table(name = "experiment_part_indicator", uniqueConstraints = @UniqueConstraint(columnNames = {"experiment_part_id", "name"}))
public class ExperimentPartIndicator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "experiment_part_id", nullable = false)
    private ExperimentPart experimentPart;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double value;

    public ExperimentPartIndicator() {}

    public ExperimentPartIndicator(String name, double value) {
        this.name = name;
        this.value = value;
    }

    public void setExperimentPart(ExperimentPart experimentPart) {
        this.experimentPart = experimentPart;
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
