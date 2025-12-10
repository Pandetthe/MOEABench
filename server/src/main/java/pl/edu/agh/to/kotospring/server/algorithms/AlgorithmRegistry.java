package pl.edu.agh.to.kotospring.server.algorithms;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.TypedProperties;
import org.moeaframework.problem.Problem;

import java.util.Properties;
import java.util.Set;

public interface AlgorithmRegistry {
    Set<String> getAllRegisteredAlgorithms();
    Algorithm getAlgorithm(String name, TypedProperties parameters, Problem problem);
    Algorithm getAlgorithm(String name, Properties parameters, Problem problem);
}
