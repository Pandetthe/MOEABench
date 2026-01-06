package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;
import pl.edu.agh.to.kotospring.server.entities.embeddables.RunId;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExperimentPartRepository extends JpaRepository<ExperimentPart, Long> {
    @Query("""
        select distinct pa from ExperimentPart pa
        left join fetch pa.indicators ind
        left join fetch pa.solutions sol
        left join fetch sol.objectives obj
        left join fetch sol.constraints con
        left join fetch sol.variables var
        where pa.id = :id and pa.experimentRun.id = :experimentId
        """)
    Optional<ExperimentPart> findWithFullSolutionById(Long experimentId, Long id);

    @Query("""
        select distinct p from ExperimentPart p
        where p.id = :id and p.id = :runId
        """)
    Optional<ExperimentPart> findByExperimentIdAndId(RunId runId, Long id);

    List<ExperimentPart> findAllByExperimentRunId(RunId id);
    long countByExperimentRunIdAndStatusIn(RunId id, Collection<ExperimentPartStatus> statuses);
}