package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;

@Entity
public class ExperimentPartAlgorithmParameter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "experiment_part_id", nullable = false)
    private ExperimentPart experimentPart;

    @Column(nullable = false)
    private String key;

    @Column(nullable = false)
    private String value;

    public ExperimentPartAlgorithmParameter() {}

    public ExperimentPartAlgorithmParameter(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public ExperimentPart getexperimentPart() {
        return experimentPart;
    }

    public void setExperimentPart(ExperimentPart experimentPart) {
        this.experimentPart = experimentPart;
    }

    public Object getKey() {
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

