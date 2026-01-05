package pl.edu.agh.to.kotospring.server.services.implementation;

import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.indicator.StandardIndicator;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.kotospring.server.exceptions.IndicatorNotFoundException;
import pl.edu.agh.to.kotospring.server.services.interfaces.IndicatorRegistryService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public final class IndicatorRegistryServiceImpl implements IndicatorRegistryService {
    @Override
    public Set<String> getAllRegisteredIndicators() {
        return Arrays.stream(StandardIndicator.values())
                .map(Enum::name)
                .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
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
        Set<String> invalid = indicatorNames.stream()
                .filter(name -> !isValidIndicator(name))
                .collect(Collectors.toSet());
        if (!invalid.isEmpty()) {
            throw new IndicatorNotFoundException(invalid);
        }
        EnumSet<StandardIndicator> indicators = indicatorNames.stream()
                .map(StandardIndicator::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(StandardIndicator.class)));
        return getIndicators(indicators, problem, referenceSet);
    }

    private boolean isValidIndicator(String name) {
        try {
            StandardIndicator.valueOf(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
