package pl.edu.agh.to.kotospring.server.entities;

import jakarta.persistence.*;
import pl.edu.agh.to.kotospring.server.entities.embeddables.RunId;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "experiment_run")
public class ExperimentRun {
    @EmbeddedId
    private RunId id;

    @ManyToOne(optional = false)
    @MapsId("experimentId")
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExperimentRunStatus status;

    @Column(name = "started_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime finishedAt;

    @OneToMany(mappedBy = "experimentRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<ExperimentPartExecution> parts = new HashSet<>();

    public ExperimentRun() {
        this.status = ExperimentRunStatus.QUEUED;
        this.id = new RunId();
    }

    public ExperimentRun(Experiment experiment, Long runNo) {
        this();
        if (experiment == null || experiment.getId() == null) {
            throw new IllegalStateException(
                    "Experiment must be persisted and must have id before creating ExperimentRun!");
        }
        this.id = new RunId(experiment.getId(), runNo);
        this.experiment = experiment;
    }

    public RunId getId() {
        return id;
    }

    public Long getExperimentId() {
        return this.id.getExperimentId();
    }

    public Long getRunNo() {
        return this.id.getRunNo();
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
        if (experiment != null && !experiment.getRuns().contains(this)) {
            experiment.addRun(this);
        }
        if (experiment != null && this.id != null) {
            this.id.setExperimentId(experiment.getId());
        }
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public void setStatus(ExperimentRunStatus status) {
        this.status = status;
    }

    public ExperimentRunStatus getStatus() {
        return this.status;
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

    public Set<ExperimentPartExecution> getParts() {
        return Collections.unmodifiableSet(parts);
    }

    public void addPart(ExperimentPartExecution part) {
        if (part == null) {
            throw new IllegalArgumentException("part must not be null");
        }
        if (this.parts.add(part)) {
            part.setExperimentRun(this);
        }
    }

    public void removePart(ExperimentPartExecution part) {
        if (part == null)
            return;
        if (this.parts.remove(part)) {
            part.setExperimentRun(null);
        }
    }
}
