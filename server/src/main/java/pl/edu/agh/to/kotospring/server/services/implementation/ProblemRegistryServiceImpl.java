package pl.edu.agh.to.kotospring.server.services.implementation;

import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.spi.RegisteredProblemProvider;
import org.moeaframework.problem.Problem;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.kotospring.server.services.interfaces.ProblemRegistryService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public final class ProblemRegistryServiceImpl implements ProblemRegistryService {
    private final List<RegisteredProblemProvider> providers;

    public ProblemRegistryServiceImpl(List<RegisteredProblemProvider> providers) {
        this.providers = providers;
    }

    @Override
    public Set<String> getAllRegisteredProblems() {
        return providers.stream()
                .map(RegisteredProblemProvider::getRegisteredProblems)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
    }

    @Override
    public Optional<Problem> getProblem(String name) {
        return providers.stream()
                .map(provider -> instantiateProblem(provider, name))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static Optional<Problem> instantiateProblem(RegisteredProblemProvider provider, String name) {
        try {
            return Optional.ofNullable(provider.getProblem(name));
        } catch (ServiceConfigurationError ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<NondominatedPopulation> getReferenceSet(String name) {
        return providers.stream()
                .map(provider -> instantiateReferenceSet(provider, name))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static Optional<NondominatedPopulation> instantiateReferenceSet(RegisteredProblemProvider provider, String name) {
        try {
            return Optional.ofNullable(provider.getReferenceSet(name));
        } catch (ServiceConfigurationError ex) {
            return Optional.empty();
        }
    }
}
