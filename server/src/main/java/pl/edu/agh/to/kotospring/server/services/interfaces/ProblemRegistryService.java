package pl.edu.agh.to.kotospring.server.services.interfaces;

import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;
import pl.edu.agh.to.kotospring.server.exceptions.ProblemNotFoundException;

import java.util.Set;

public interface ProblemRegistryService {
    Set<String> getAllRegisteredProblems();
    Problem getProblem(String name) throws ProblemNotFoundException;
    NondominatedPopulation getReferenceSet(String problemName) throws ProblemNotFoundException;
}