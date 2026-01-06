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
        select distinct ru from ExperimentRun ru
        left join fetch ru.parts pa
        left join fetch pa.parameters par
        left join fetch pa.indicators ind
        where ru.id = :id
        """)
    Optional<ExperimentRun> findWithPartsById(RunId id);

    @Query("""
        select distinct ru from ExperimentRun ru
        left join fetch ru.parts pa
        left join fetch pa.indicators ind
        left join fetch pa.solutions sol
        left join fetch sol.objectives obj
        left join fetch sol.constraints con
        left join fetch sol.variables var
        where ru.id = :id
        """)
    Optional<ExperimentRun> findWithFullSolutionById(RunId id);

    List<ExperimentRun> findAllByIdExperimentId(Long experimentId);
    long countByIdExperimentIdAndStatusIn(Long experimentId, Collection<ExperimentRunStatus> statuses);
}
