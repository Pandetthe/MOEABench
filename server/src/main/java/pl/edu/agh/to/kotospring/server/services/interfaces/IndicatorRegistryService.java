package pl.edu.agh.to.kotospring.server.services.interfaces;

import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.indicator.StandardIndicator;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;
import pl.edu.agh.to.kotospring.server.exceptions.IndicatorNotFoundException;

import java.util.EnumSet;
import java.util.Set;

public interface IndicatorRegistryService {
    Set<String> getAllRegisteredIndicators();
    Indicators getIndicators(EnumSet<StandardIndicator> indicatorNames, Problem problem,
                             NondominatedPopulation referenceSet);
    Indicators getIndicators(Set<String> indicatorNames, Problem problem,
                             NondominatedPopulation referenceSet) throws IndicatorNotFoundException;
}