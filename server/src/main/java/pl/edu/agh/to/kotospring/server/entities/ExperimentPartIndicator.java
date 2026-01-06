package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "experiment_part_indicator", uniqueConstraints = @UniqueConstraint(columnNames = {"experiment_part_id", "name"}))
public class ExperimentPartIndicator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "experiment_part_id", nullable = false)
    private ExperimentPart experimentPart;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "value", nullable = false)
    private double value;

    public ExperimentPartIndicator() {}
    public ExperimentPartIndicator(String name, double value) {
        this.name = name;
        this.value = value;
    }

    public Long getId() { return id; }

    public ExperimentPart getExperimentPart() {
        return experimentPart;
    }
    public void setExperimentPart(ExperimentPart experimentPart) {
        this.experimentPart = experimentPart;
        if (experimentPart != null && !experimentPart.getIndicators().contains(this)) {
            experimentPart.addIndicator(this);
        }
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public double getValue() {
        return value;
    }
    public void setValue(double value) {
        this.value = value;
    }
}
