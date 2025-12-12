package pl.edu.agh.to.kotospring.server.services.implementation;

import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.indicator.StandardIndicator;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.kotospring.server.exceptions.IndicatorNotFoundException;
import pl.edu.agh.to.kotospring.server.services.interfaces.IndicatorRegistryService;

import java.util.*;

@Service
public final class IndicatorRegistryServiceImpl implements IndicatorRegistryService {
    @Override
    public Set<String> getAllRegisteredIndicators() {
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (StandardIndicator indicator : StandardIndicator.values()) {
            result.add(indicator.name());
        }
        return result;
    }

    @Override
    public Indicators getIndicators(EnumSet<StandardIndicator> indicatorNames, Problem problem, NondominatedPopulation referenceSet) {
        Indicators indicators = Indicators.of(problem, referenceSet);
        indicators.include(indicatorNames);
        return indicators;
    }

    @Override
    public Indicators getIndicators(Set<String> indicatorNames, Problem problem, NondominatedPopulation referenceSet)
            throws IndicatorNotFoundException {
        EnumSet<StandardIndicator> indicators = EnumSet.noneOf(StandardIndicator.class);
        Set<String> invalid = new HashSet<>();
        for (String name : indicatorNames) {
            try {
                indicators.add(StandardIndicator.valueOf(name));
            } catch (IllegalArgumentException ex) {
                invalid.add(name);
            }
        }
        if (!invalid.isEmpty()) {
            throw new IndicatorNotFoundException(invalid);
        }
        return getIndicators(indicators, problem, referenceSet);
    }
}
