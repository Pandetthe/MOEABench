package pl.edu.agh.to.kotospring.server.services.implementation;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.TypedProperties;
import org.moeaframework.core.spi.RegisteredAlgorithmProvider;
import org.moeaframework.problem.Problem;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.kotospring.server.services.interfaces.AlgorithmRegistryService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public final class AlgorithmRegistryServiceImpl implements AlgorithmRegistryService {
    private final List<RegisteredAlgorithmProvider> providers;

    public AlgorithmRegistryServiceImpl(List<RegisteredAlgorithmProvider> providers) {
        this.providers = providers;
    }

    @Override
    public Set<String> getAllRegisteredAlgorithms() {
        return providers.stream()
                .map(RegisteredAlgorithmProvider::getRegisteredAlgorithms)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
    }

    @Override
    public Optional<Algorithm> getAlgorithm(String name, TypedProperties parameters, Problem problem) {
        return providers.stream()
                .map(provider -> instantiateAlgorithm(provider, name, parameters, problem))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static Optional<Algorithm> instantiateAlgorithm(RegisteredAlgorithmProvider provider,
                                                            String name, TypedProperties parameters, Problem problem) {
        try {
            return Optional.ofNullable(provider.getAlgorithm(name, parameters, problem));
        } catch (ServiceConfigurationError ex) {
            return Optional.empty();
        }
    }

    @Override
    public TypedProperties createTypedProperties(Map<String, Object> parameters) {
        Properties properties = new Properties(parameters.size());
        properties.putAll(parameters);
        return new TypedProperties(properties);
    }
}
