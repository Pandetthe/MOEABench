package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPartSolution;

@Repository
public interface ExperimentPartSolutionEntityRepository extends JpaRepository<ExperimentPartSolution, Long> {
}
