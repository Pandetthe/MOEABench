package pl.edu.agh.to.kotospring.server.problems;

import org.moeaframework.core.spi.ProviderNotFoundException;
import org.moeaframework.core.spi.RegisteredAlgorithmProvider;
import org.moeaframework.core.spi.RegisteredProblemProvider;
import org.moeaframework.problem.Problem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
public class ProblemRegistryService {
    private final List<RegisteredProblemProvider> providers;

    public ProblemRegistryService(List<RegisteredProblemProvider> providers) {
        this.providers = providers;
    }

    public Set<String> getAllRegisteredProblems() {
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (RegisteredProblemProvider provider : providers) {
            result.addAll(provider.getRegisteredProblems());
        }
        return result;
    }

    public Problem getProblem(String name) {
        for (RegisteredProblemProvider provider : providers) {
            if (provider.getRegisteredProblems().contains(name)) {
                return provider.getProblem(name);
            }
        }
        throw new ProviderNotFoundException(name);
    }
}
