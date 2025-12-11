package pl.edu.agh.to.kotospring.server.experiments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExperimentRepository extends JpaRepository<ExperimentEntity, Long> {
}
