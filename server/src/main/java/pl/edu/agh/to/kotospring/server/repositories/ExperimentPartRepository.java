package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;

import java.util.Optional;

@Repository
public interface ExperimentPartRepository extends JpaRepository<ExperimentPart, Long> {
    @Query("""
        select distinct p from ExperimentPart p
        left join fetch p.solutions s
        left join fetch p.indicators
        left join fetch s.constraints
        left join fetch s.objectives
        left join fetch s.variables
        where p.id = :id and p.experiment.id = :experimentId
        """)
    Optional<ExperimentPart> findWithFullSolutionById(Long experimentId, Long id);

    @Query("""
        select distinct p from ExperimentPart p
        where p.id = :id and p.experiment.id = :experimentId
        """)
    Optional<ExperimentPart> findByExperimentIdAndId(Long experimentId, Long id);
}