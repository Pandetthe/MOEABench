package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Experiment {
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
    private final List<ExperimentPart> parts;

    public Experiment(OffsetDateTime queuedAt,
                      List<ExperimentPart> parts) {
        this.status = ExperimentStatus.QUEUED;
        this.queuedAt = queuedAt;
        this.parts = parts;
    }

    public Experiment() {
        parts = new ArrayList<>();
    };

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
    public List<ExperimentPart> getParts() {
        return parts;
    }
}
