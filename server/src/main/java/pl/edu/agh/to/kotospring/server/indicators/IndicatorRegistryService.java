package pl.edu.agh.to.kotospring.server.indicators;

import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.indicator.StandardIndicator;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public final class IndicatorRegistryService implements IndicatorRegistry {
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
        if (indicatorNames == null) {
            throw new IllegalArgumentException("indicatorNames must not be null");
        }
        Indicators indicators = Indicators.of(problem, referenceSet);
        indicators.include(indicatorNames);
        return indicators;
    }

    @Override
    public Indicators getIndicators(Set<String> indicatorNames, Problem problem, NondominatedPopulation referenceSet) throws IllegalArgumentException {
        if (indicatorNames == null) {
            throw new IllegalArgumentException("indicatorNames must not be null");
        }
        EnumSet<StandardIndicator> indicators = EnumSet.noneOf(StandardIndicator.class);
        List<String> invalid = new ArrayList<>();
        for (String name : indicatorNames) {
            try {
                indicators.add(StandardIndicator.valueOf(name));
            } catch (IllegalArgumentException ex) {
                invalid.add(name);
            }
        }
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("Invalid indicators: " + String.join(", ", invalid));
        }
        return getIndicators(indicators, problem, referenceSet);
    }
}
