package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.edu.agh.to.kotospring.server.entities.ExperimentGroup;

public interface ExperimentGroupRepository extends JpaRepository<ExperimentGroup, Long> {
}
