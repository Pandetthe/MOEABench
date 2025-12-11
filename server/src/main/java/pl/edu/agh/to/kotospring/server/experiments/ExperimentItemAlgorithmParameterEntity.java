package pl.edu.agh.to.kotospring.server.experiments;

import jakarta.persistence.*;

@Entity
public class ExperimentItemAlgorithmParameterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "experiment_item_id", nullable = false)
    private ExperimentItemEntity experimentItem;

    @Column(nullable = false)
    private String key;

    @Column(nullable = false)
    private String value;

    public ExperimentItemAlgorithmParameterEntity() {}

    public Long getId() {
        return id;
    }

    public ExperimentItemEntity getExperimentItem() {
        return experimentItem;
    }

    public void setExperimentItem(ExperimentItemEntity experimentItem) {
        this.experimentItem = experimentItem;
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

