package pl.edu.agh.to.kotospring.server.services.implementation;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.TypedProperties;
import org.moeaframework.core.spi.RegisteredAlgorithmProvider;
import org.moeaframework.problem.Problem;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.kotospring.server.exceptions.AlgorithmNotFoundException;
import pl.edu.agh.to.kotospring.server.services.interfaces.AlgorithmRegistryService;

import java.util.*;

@Service
public final class AlgorithmRegistryServiceImpl implements AlgorithmRegistryService {
    private final List<RegisteredAlgorithmProvider> providers;

    public AlgorithmRegistryServiceImpl(List<RegisteredAlgorithmProvider> providers) {
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
    public Algorithm getAlgorithm(String name, TypedProperties parameters, Problem problem) throws AlgorithmNotFoundException {
        for (RegisteredAlgorithmProvider provider : providers) {
            Algorithm algorithm = instantiateAlgorithm(provider, name, parameters, problem);
            if (algorithm != null) {
                return algorithm;
            }
        }
        throw new AlgorithmNotFoundException(name);
    }

    private static Algorithm instantiateAlgorithm(RegisteredAlgorithmProvider provider,
                                                  String name, TypedProperties parameters, Problem problem) {
        try {
            return provider.getAlgorithm(name, parameters, problem);
        } catch (ServiceConfigurationError ex) {
            return null;
        }
    }

    @Override
    public TypedProperties createTypedProperties(Map<String, Object> parameters) {
        Properties properties = new Properties(parameters.size());
        properties.putAll(parameters);
        return new TypedProperties(properties);
    }
}
