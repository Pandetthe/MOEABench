package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.kotospring.server.entities.ExperimentRun;
import pl.edu.agh.to.kotospring.server.entities.embeddables.RunId;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentRunStatus;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, RunId> {
  @Query("""
      select distinct ru from ExperimentRun ru
      left join fetch ru.parts pa
      left join fetch pa.experimentPart def
      left join fetch def.parameters par
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

  @Query("""
      select ru from ExperimentRun ru
      where (:algorithm is null or exists (
          select 1 from ExperimentPartExecution pa
          join pa.experimentPart def
          where pa.experimentRun = ru and def.algorithm = :algorithm))
        and (:problem is null or exists (
          select 1 from ExperimentPartExecution pa
          join pa.experimentPart def
          where pa.experimentRun = ru and def.problem = :problem))
        and (:indicator is null or exists (
          select 1 from ExperimentPartExecution pa
          join pa.indicators ind
          where pa.experimentRun = ru and ind.name = :indicator))
        and (:status is null or ru.status = :status)
        and (cast(:start as timestamp) is null or ru.startedAt >= :start)
        and (cast(:end as timestamp) is null or ru.finishedAt <= :end)
      """)
  Page<ExperimentRun> findAllFiltered(
      String algorithm,
      String problem,
      String indicator,
      ExperimentRunStatus status,
      OffsetDateTime start,
      OffsetDateTime end,
      Pageable pageable);

  Optional<ExperimentRunStatus> findStatusById(RunId id);

  List<ExperimentRun> findAllByIdExperimentId(Long experimentId);

  long countByIdExperimentIdAndStatusIn(Long experimentId, Collection<ExperimentRunStatus> statuses);
}
