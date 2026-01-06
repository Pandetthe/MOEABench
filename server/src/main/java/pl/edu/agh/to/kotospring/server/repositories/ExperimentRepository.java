package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.edu.agh.to.kotospring.server.entities.Experiment;

import java.util.Optional;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    @Query("""
        select distinct f from Experiment f
        left join fetch f.runs e
        left join fetch e.parts p
        left join fetch p.parameters par
        left join fetch p.indicators ind
        where f.id = :id
        """)
    Optional<Experiment> findWithRunsById(Long id);
}
