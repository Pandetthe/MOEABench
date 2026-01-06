package pl.edu.agh.to.kotospring.server.entities.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RunId implements Serializable {
    @Column(name = "experiment_id")
    private Long experimentId;
    @Column(name = "run_no")
    private Long runNo;

    public RunId() {}
    public RunId(Long runNo) {
        this.runNo = runNo;
    }
    public RunId(Long experimentId, Long runNo) {
        this.runNo = runNo;
        this.experimentId = experimentId;
    }

    public Long getExperimentId() {
        return experimentId;
    }
    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    public Long getRunNo() {
        return runNo;
    }
    public void setRunNo(Long runNo) {
        this.runNo = runNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RunId that)) return false;
        return Objects.equals(experimentId, that.experimentId) &&
                Objects.equals(runNo, that.runNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(experimentId, runNo);
    }
}