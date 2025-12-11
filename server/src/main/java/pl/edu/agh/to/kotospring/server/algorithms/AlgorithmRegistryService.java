package pl.edu.agh.to.kotospring.server.algorithms;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.TypedProperties;
import org.moeaframework.core.spi.ProviderNotFoundException;
import org.moeaframework.core.spi.RegisteredAlgorithmProvider;
import org.moeaframework.problem.Problem;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public final class AlgorithmRegistryService implements AlgorithmRegistry {
    private final List<RegisteredAlgorithmProvider> providers;

    public AlgorithmRegistryService(List<RegisteredAlgorithmProvider> providers) {
        this.providers = providers;
    }

    @Override
    public Set<String> getAllRegisteredAlgorithms() {
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (RegisteredAlgorithmProvider provider : providers) {
            result.addAll(provider.getRegisteredAlgorithms());
        }
        return result;
    }

    @Override
    public Algorithm getAlgorithm(String name, TypedProperties parameters, Problem problem) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Algorithm name must be provided");
        }
        for (RegisteredAlgorithmProvider provider : providers) {
            Algorithm algorithm = instantiateAlgorithm(provider, name, parameters, problem);
            if (algorithm != null) {
                return algorithm;
            }
        }
        throw new ProviderNotFoundException("Algorithm not found: " + name);
    }

    private static Algorithm instantiateAlgorithm(RegisteredAlgorithmProvider provider, String name, TypedProperties parameters, Problem problem) {
        try {
            return provider.getAlgorithm(name, parameters, problem);
        } catch (ServiceConfigurationError ex) {
            return null;
        }
    }

    @Override
    public Algorithm getAlgorithm(String name, Properties parameters, Problem problem) {
        TypedProperties typedProperties = new TypedProperties(parameters);
        return getAlgorithm(name, typedProperties, problem);
    }
}
