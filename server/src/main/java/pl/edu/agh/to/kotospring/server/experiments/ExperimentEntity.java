package pl.edu.agh.to.kotospring.server.experiments;

import jakarta.persistence.*;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.ExperimentStatus;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
public class ExperimentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperimentStatus status;

    @Column(columnDefinition = "TIMESTAMPTZ",  nullable = false)
    private OffsetDateTime queuedAt;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime startedAt;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime finishedAt;

    @OneToMany(mappedBy = "experiment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExperimentItemEntity> items;


    public ExperimentEntity() {};
    public void setStatus(ExperimentStatus status) {
        this.status = status;
    }

    public ExperimentStatus getStatus() {
        return this.status;
    }


    public Long getId() {
        return id;
    }

    public void setQueuedAt(OffsetDateTime queued_at) {
        this.queuedAt = queued_at;
    }
    public OffsetDateTime getQueuedAt() {
        return queuedAt;
    }
    public void setStartedAt(OffsetDateTime started_at) {
        this.startedAt = started_at;
    }
    public OffsetDateTime getStartedAt() {
        return startedAt;
    }
    public void setFinishedAt(OffsetDateTime finished_at) {
        this.finishedAt = finished_at;
    }
    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }
    public List<ExperimentItemEntity> getItems() {
        return items;
    }

    public void setItems(List<ExperimentItemEntity> items) {
        this.items = items;
    }

}
