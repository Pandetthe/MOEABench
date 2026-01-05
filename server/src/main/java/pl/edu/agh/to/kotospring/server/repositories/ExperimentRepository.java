package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.kotospring.server.entities.Experiment;

import java.util.Optional;

@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
    @Query("""
        select distinct e from Experiment e
        left join fetch e.parts p
        left join fetch p.parameters par
        left join fetch p.indicators ind
        where e.id = :id
        """)
    Optional<Experiment> findWithPartsById(Long id);

    @Query("""
        select distinct e from Experiment e
        left join fetch e.parts p
        left join fetch p.solutions s
        left join fetch s.variables v
        left join fetch s.constraints c
        left join fetch s.objectives o
        left join fetch p.indicators i
        where e.id = :id
        """)
    Optional<Experiment> findWithFullSolutionById(Long id);
}
