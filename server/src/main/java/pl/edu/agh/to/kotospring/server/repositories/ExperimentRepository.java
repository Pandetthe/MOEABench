package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.edu.agh.to.kotospring.server.entities.Experiment;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
    @Query("""
        select distinct ex from Experiment ex
        join fetch ex.runs ru
        join fetch ru.parts pa
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

    @Query("""
    select distinct ex from Experiment ex
    join ex.runs ru
    join ru.parts pa
    left join pa.indicators ind
    where (:algorithm is null or pa.algorithm = :algorithm)
      and (:problem is null or pa.problem = :problem)
      and (:status is null or ex.status = :status)
      and (:indicator is null or ind.name = :indicator)
      and (cast(:start as timestamp) is null or ex.queuedAt >= :start)
      and (cast(:end as timestamp) is null or ex.queuedAt <= :end)
    """)
    List<Experiment> findAllFiltered(String algorithm, String problem, String indicator,
                                     ExperimentStatus status, OffsetDateTime start, OffsetDateTime end);
    
    Optional<ExperimentStatus> findStatusById(Long id);
}
