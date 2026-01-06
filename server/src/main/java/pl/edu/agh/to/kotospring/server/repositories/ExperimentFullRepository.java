package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.server.entities.ExperimentFull;

import java.util.Optional;

public interface ExperimentFullRepository extends JpaRepository<ExperimentFull, Long> {

    @Query("""
        select distinct f from ExperimentFull f
        left join fetch f.runs e
        left join fetch e.parts p
        left join fetch p.parameters par
        left join fetch p.indicators ind
        where f.id = :id
        """)
    Optional<ExperimentFull> findWithRunsById(Long id);
}
