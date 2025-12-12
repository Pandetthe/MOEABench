package pl.edu.agh.to.kotospring.server.services.interfaces;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.TypedProperties;
import org.moeaframework.problem.Problem;
import pl.edu.agh.to.kotospring.server.exceptions.AlgorithmNotFoundException;

import java.util.Map;
import java.util.Set;

public interface AlgorithmRegistryService {
    Set<String> getAllRegisteredAlgorithms();
    Algorithm getAlgorithm(String name, TypedProperties parameters, Problem problem) throws AlgorithmNotFoundException;
    TypedProperties createTypedProperties(Map<String, Object> parameters);
}
