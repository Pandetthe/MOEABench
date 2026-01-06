package pl.edu.agh.to.kotospring.server.entities;


import jakarta.persistence.*;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
public class ExperimentFull {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperimentStatus status;

    @Column(columnDefinition =  "TIMESTAMPTZ", nullable = false)
    private OffsetDateTime queuedAt;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime startedAt;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime finishedAt;

    @OneToMany(mappedBy = "experimentFull", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<Experiment> runs = new HashSet<>();

    public ExperimentFull(OffsetDateTime queuedAt,
                          Collection<Experiment> runs) {
        this.status = ExperimentStatus.QUEUED;
        this.queuedAt = Objects.requireNonNull(queuedAt, "queuedAt must not be null");
        if (runs != null) {
            for (Experiment run: runs) {
                addRun(run);
            }
        }
    }

    public ExperimentFull() {}

    public void addRun(Experiment run) {
        if (run == null) {
            throw new IllegalArgumentException("run must not be null");
        }

        this.runs.add(run);
        run.setExperimentFull(this);
    }

    public Long getId() {
        return id;
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(ExperimentStatus status) {
        this.status = status;
    }

    public OffsetDateTime getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(OffsetDateTime queuedAt) {
        this.queuedAt = queuedAt;
    }

    public Set<Experiment> getRuns() {
        return runs;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }
}
