package pl.edu.agh.to.kotospring.server.algorithms;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.TypedProperties;
import org.moeaframework.core.spi.RegisteredAlgorithmProvider;
import org.moeaframework.problem.Problem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

@Service
public class AlgorithmRegistryService {
    private final List<RegisteredAlgorithmProvider> providers;

    public AlgorithmRegistryService(List<RegisteredAlgorithmProvider> providers) {
        this.providers = providers;
    }

    public Set<String> getAllRegisteredAlgorithms() {
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (RegisteredAlgorithmProvider provider : providers) {
            result.addAll(provider.getRegisteredAlgorithms());
        }
        return result;
    }

    public Algorithm getAlgorithm(String name, TypedProperties parameters,  Problem problem) {
        for (RegisteredAlgorithmProvider provider : providers) {
            if (provider.getRegisteredAlgorithms().contains(name)) {
                return provider.getAlgorithm(name, parameters, problem);
            }
        }
        throw new IllegalArgumentException("Algorithm not found: " + name);
    }

    public Algorithm getAlgorithm(String name, Properties parameters, Problem problem) {
        TypedProperties typedProperties = new TypedProperties(parameters);
        return getAlgorithm(name, typedProperties, problem);
    }
}
