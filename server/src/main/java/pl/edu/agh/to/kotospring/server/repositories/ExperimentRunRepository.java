package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.kotospring.server.entities.ExperimentRun;
import pl.edu.agh.to.kotospring.server.entities.embeddables.RunId;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, RunId> {
    @Query("""
        select distinct e from ExperimentRun e
        left join fetch e.parts p
        left join fetch p.parameters par
        left join fetch p.indicators ind
        where e.id = :id
        """)
    Optional<ExperimentRun> findWithPartsById(RunId id);

    @Query("""
        select distinct e from ExperimentRun e
        left join fetch e.parts p
        left join fetch p.solutions s
        left join fetch s.variables v
        left join fetch s.constraints c
        left join fetch s.objectives o
        left join fetch p.indicators i
        where e.id = :id
        """)
    Optional<ExperimentRun> findWithFullSolutionById(RunId id);

    List<ExperimentRun> findAllByIdExperimentId(Long experimentId);
    long countByIdExperimentIdAndStatusIn(Long experimentId, Collection<ExperimentRunStatus> statuses);
}
