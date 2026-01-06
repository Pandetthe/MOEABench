package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.edu.agh.to.kotospring.server.entities.Experiment;

import java.util.Optional;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
    @Query("""
        select distinct ex from Experiment ex
        left join fetch ex.runs ru
        left join fetch ru.parts pa
        left join fetch pa.indicators ind
        left join fetch pa.solutions sol
        left join fetch sol.objectives obj
        left join fetch sol.constraints con
        left join fetch sol.variables var
        where ex.id = :id
        """)
    Optional<Experiment> findWithSolutionById(Long id);

    @Query("""
        select distinct ex from Experiment ex
        left join fetch ex.runs ru
        where ex.id = :id
        """)
    Optional<Experiment> findWithRunsById(Long id);
}
