package pl.edu.agh.to.kotospring.server.problems;

import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;

import java.util.Set;

public interface ProblemRegistry {
    Set<String> getAllRegisteredProblems();
    Problem getProblem(String name);
    NondominatedPopulation getReferenceSet(String problemName);
}