package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartSolutionEntity;

@Repository
public interface ExperimentPartSolutionEntityRepository extends JpaRepository<ExperimentPartSolutionEntity, Long> {
}
