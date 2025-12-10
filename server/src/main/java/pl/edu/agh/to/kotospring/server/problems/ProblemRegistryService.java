package pl.edu.agh.to.kotospring.server.problems;

import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.TypedProperties;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.spi.ProviderNotFoundException;
import org.moeaframework.core.spi.RegisteredAlgorithmProvider;
import org.moeaframework.core.spi.RegisteredProblemProvider;
import org.moeaframework.problem.Problem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.TreeSet;

@Service
public final class ProblemRegistryService implements ProblemRegistry {
    private final List<RegisteredProblemProvider> providers;

    public ProblemRegistryService(List<RegisteredProblemProvider> providers) {
        this.providers = providers;
    }

    @Override
    public Set<String> getAllRegisteredProblems() {
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (RegisteredProblemProvider provider : providers) {
            result.addAll(provider.getRegisteredProblems());
        }
        return result;
    }

    @Override
    public Problem getProblem(String name) throws ProviderNotFoundException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Problem name must be provided");
        }
        for (RegisteredProblemProvider provider : providers) {
            Problem problem = instantiateProblem(provider, name);
            if (problem != null) {
                return problem;
            }
        }
        throw new ProviderNotFoundException(name);
    }

    private static Problem instantiateProblem(RegisteredProblemProvider provider, String name) {
        try {
            return provider.getProblem(name);
        } catch (ServiceConfigurationError ex) {
            return null;
        }
    }

    @Override
    public NondominatedPopulation getReferenceSet(String name) throws ProviderNotFoundException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Reference set name must be provided");
        }
        for (RegisteredProblemProvider provider : providers) {
            NondominatedPopulation referenceSet = instantiateReferenseSet(provider, name);
            if (referenceSet != null) {
                return referenceSet;
            }
        }
        throw new ProviderNotFoundException(name);
    }

    private static NondominatedPopulation instantiateReferenseSet(RegisteredProblemProvider provider, String name) {
        try {
            return provider.getReferenceSet(name);
        } catch (ServiceConfigurationError ex) {
            return null;
        }
    }
}
