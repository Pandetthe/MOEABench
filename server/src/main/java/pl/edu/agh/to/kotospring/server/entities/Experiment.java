package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;

import java.time.OffsetDateTime;
import java.util.*;

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
    private final Set<ExperimentPart> parts = new HashSet<>();

    public Experiment(OffsetDateTime queuedAt,
                      Collection<ExperimentPart> parts) {
        this.status = ExperimentStatus.QUEUED;
        this.queuedAt =  Objects.requireNonNull(queuedAt, "queuedAt must not be null");
        if (parts != null) {
            for (ExperimentPart part : parts) {
                addPart(part);
            }
        }
    }

    public Experiment() {
    }

    public void addPart(ExperimentPart part) {
        if (part == null) {
            throw new IllegalArgumentException("part must not be null");
        }
        this.parts.add(part);
        part.setExperiment(this);
    }

    public Long getId() {
        return id;
    }

    public void setStatus(ExperimentStatus status) {
        this.status = status;
    }

    public ExperimentStatus getStatus() {
        return this.status;
    }

    public void setQueuedAt(OffsetDateTime queuedAt) {
        this.queuedAt = queuedAt;
    }
    public OffsetDateTime getQueuedAt() {
        return queuedAt;
    }
    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }
    public OffsetDateTime getStartedAt() {
        return startedAt;
    }
    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }
    public Set<ExperimentPart> getParts() {
        return Collections.unmodifiableSet(parts);
    }
}
