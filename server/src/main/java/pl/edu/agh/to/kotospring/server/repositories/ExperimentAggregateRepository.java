package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartExecution;

import java.util.List;

@Repository
public interface ExperimentAggregateRepository extends JpaRepository<ExperimentPartExecution, Long> {
    @Query(value = """
            select epe.experiment_part_id as definition_id,
                epi.name as indicator,
                min(epi.value) as min_value,
                max(epi.value) as max_value,
                avg(epi.value) as mean_value,
                percentile_cont(0.5) within group (order by epi.value) as median_value,
                (percentile_cont(0.75) WITHIN group (order by epi.value)-percentile_cont(0.25) within group (order by epi.value)) as iqr_value,
                coalesce(stddev_samp(epi.value), 0.0) as stddev_value
            from experiment_part_indicator epi
            join experiment_part_execution epe
              on epe.id = epi.experiment_part_id
            where epe.experiment_id = :experimentId
            group BY epe.experiment_part_id, epi.name
            """, nativeQuery = true)
    List<Object[]> findIndicatorAggregatesByExperimentId(long experimentId);

    @Query(value = """
            select ep.algorithm,
                ep.problem,
                epi.name as indicator,
                min(epi.value) as min_value,
                max(epi.value) as max_value,
                avg(epi.value) as mean_value,
                percentile_cont(0.5) within group (order by epi.value) as median_value,
                (percentile_cont(0.75) WITHIN group (order by epi.value)-percentile_cont(0.25) within group (order by epi.value)) as iqr_value,
                coalesce(stddev_samp(epi.value), 0.0) as stddev_value
            from experiment_part_indicator epi
            join experiment_part_execution epe on epe.id = epi.experiment_part_id
            join experiment_part ep on ep.id = epe.experiment_part_id
            join experiment_run er on er.experiment_id = epe.experiment_id and er.run_no = epe.run_no
            join experiment_group_runs egr on egr.experiment_id = er.experiment_id and egr.run_no = er.run_no
            where egr.group_id = :groupId
            group BY ep.algorithm, ep.problem, epi.name
            """, nativeQuery = true)
    List<Object[]> findIndicatorAggregatesByExperimentGroupId(@Param("groupId") long groupId);
}
