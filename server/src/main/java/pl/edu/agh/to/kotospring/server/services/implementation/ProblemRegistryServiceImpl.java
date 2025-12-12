package pl.edu.agh.to.kotospring.server.services.implementation;

import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.spi.RegisteredProblemProvider;
import org.moeaframework.problem.Problem;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.kotospring.server.exceptions.ProblemNotFoundException;
import pl.edu.agh.to.kotospring.server.services.interfaces.ProblemRegistryService;

import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.TreeSet;

@Service
public final class ProblemRegistryServiceImpl implements ProblemRegistryService {
    private final List<RegisteredProblemProvider> providers;

    public ProblemRegistryServiceImpl(List<RegisteredProblemProvider> providers) {
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
    public Problem getProblem(String name) throws ProblemNotFoundException {
        for (RegisteredProblemProvider provider : providers) {
            Problem problem = instantiateProblem(provider, name);
            if (problem != null) {
                return problem;
            }
        }
        throw new ProblemNotFoundException(name);
    }

    private static Problem instantiateProblem(RegisteredProblemProvider provider, String name) {
        try {
            return provider.getProblem(name);
        } catch (ServiceConfigurationError ex) {
            return null;
        }
    }

    @Override
    public NondominatedPopulation getReferenceSet(String name) throws ProblemNotFoundException {
        for (RegisteredProblemProvider provider : providers) {
            NondominatedPopulation referenceSet = instantiateReferenceSet(provider, name);
            if (referenceSet != null) {
                return referenceSet;
            }
        }
        throw new ProblemNotFoundException(name);
    }

    private static NondominatedPopulation instantiateReferenceSet(RegisteredProblemProvider provider, String name) {
        try {
            return provider.getReferenceSet(name);
        } catch (ServiceConfigurationError ex) {
            return null;
        }
    }
}
