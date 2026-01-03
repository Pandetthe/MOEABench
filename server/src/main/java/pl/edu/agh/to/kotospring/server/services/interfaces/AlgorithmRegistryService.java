package pl.edu.agh.to.kotospring.server.services.interfaces;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.TypedProperties;
import org.moeaframework.problem.Problem;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface AlgorithmRegistryService {
    Set<String> getAllRegisteredAlgorithms();
    Optional<Algorithm> getAlgorithm(String name, TypedProperties parameters, Problem problem);
    TypedProperties createTypedProperties(Map<String, Object> parameters);
}
