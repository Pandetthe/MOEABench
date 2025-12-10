package pl.edu.agh.to.kotospring.server.indicators;

import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.indicator.StandardIndicator;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IndicatorRegistryService {
    public Set<String> getAllRegisteredIndicators() {
        Set<String> result =  new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (StandardIndicator indicator : StandardIndicator.values()) {
            result.add(indicator.name());
        }
        return result;
    }

    public Indicators getIndicators(EnumSet<StandardIndicator> indicatorNames, Problem problem, NondominatedPopulation referenceSet) {
        Indicators indicators = Indicators.of(problem, referenceSet);
        indicators.include(indicatorNames);
        return indicators;
    }

    public Indicators getIndicators(Set<String> indicatorNames, Problem problem, NondominatedPopulation referenceSet) throws IllegalArgumentException {
        EnumSet<StandardIndicator> indicators = EnumSet.noneOf(StandardIndicator.class);
        for (String name : indicatorNames) {
            indicators.add(StandardIndicator.valueOf(name));
        }
        return getIndicators(indicators, problem, referenceSet);
    }
}
