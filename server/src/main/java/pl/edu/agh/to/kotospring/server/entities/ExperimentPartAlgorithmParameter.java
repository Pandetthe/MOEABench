package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "experiment_part_algorithm_parameter", uniqueConstraints = @UniqueConstraint(columnNames = {"experiment_part_id", "key"}))
public class ExperimentPartAlgorithmParameter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "experiment_part_id", nullable = false)
    private ExperimentPart experimentPart;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "value", nullable = false)
    private String value;

    public ExperimentPartAlgorithmParameter() {}
    public ExperimentPartAlgorithmParameter(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public ExperimentPart getExperimentPart() {
        return experimentPart;
    }
    public void setExperimentPart(ExperimentPart experimentPart) {
        this.experimentPart = experimentPart;
        if (experimentPart != null && !experimentPart.getParameters().contains(this)) {
            experimentPart.addParameter(this);
        }
    }

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
}

