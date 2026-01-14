package pl.edu.agh.to.kotospring.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;

import java.util.List;

@Repository
public interface ExperimentPartRepository extends JpaRepository<ExperimentPart, Long> {
    List<ExperimentPart> findAllByExperimentId(Long experimentId);
}
