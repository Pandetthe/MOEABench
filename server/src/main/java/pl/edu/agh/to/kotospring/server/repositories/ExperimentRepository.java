package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.kotospring.server.entities.Experiment;

@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
}
