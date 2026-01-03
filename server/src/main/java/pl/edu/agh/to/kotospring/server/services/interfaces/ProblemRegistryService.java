package pl.edu.agh.to.kotospring.server.services.interfaces;

import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;

import java.util.Optional;
import java.util.Set;

public interface ProblemRegistryService {
    Set<String> getAllRegisteredProblems();
    Optional<Problem> getProblem(String name);
    Optional<NondominatedPopulation> getReferenceSet(String problemName);
}