package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "experiment")
public class Experiment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExperimentStatus status;

    @Column(name = "queued_at", columnDefinition = "TIMESTAMPTZ", nullable = false)
    private OffsetDateTime queuedAt;

    @Column(name = "started_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime finishedAt;

    @OneToMany(mappedBy = "experiment", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<ExperimentRun> runs = new HashSet<>();

    @OneToMany(mappedBy = "experiment", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<ExperimentPart> parts = new HashSet<>();

    @Column(name = "run_count")
    private long runCount;

    public Experiment() {
        this.status = ExperimentStatus.QUEUED;
    }

    public Experiment(OffsetDateTime queuedAt, long runCount) {
        this();
        this.queuedAt = Objects.requireNonNull(queuedAt, "queuedAt must not be null");
        this.runCount = runCount;
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

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Set<ExperimentRun> getRuns() {
        return runs;
    }

    public void addRun(ExperimentRun run) {
        if (run == null) {
            throw new IllegalArgumentException("run must not be null");
        }
        if (this.runs.add(run)) {
            run.setExperiment(this);
            runCount++;
        }
    }

    public void removeRun(ExperimentRun run) {
        if (run == null)
            return;
        if (this.runs.remove(run)) {
            run.setExperiment(null);
            runCount--;
        }
    }

    public Set<ExperimentPart> getParts() {
        return Collections.unmodifiableSet(parts);
    }

    public void addPart(ExperimentPart part) {
        if (part == null) {
            throw new IllegalArgumentException("part must not be null");
        }
        if (this.parts.add(part)) {
            part.setExperiment(this);
        }
    }

    public void removePart(ExperimentPart part) {
        if (part == null)
            return;
        if (this.parts.remove(part)) {
            part.setExperiment(null);
        }
    }

    public long getRunCount() {
        return runCount;
    }
}
