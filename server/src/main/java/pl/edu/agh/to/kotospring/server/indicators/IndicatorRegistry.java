package pl.edu.agh.to.kotospring.server.indicators;

import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.indicator.StandardIndicator;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;

import java.util.EnumSet;
import java.util.Set;

public interface IndicatorRegistry {
    Set<String> getAllRegisteredIndicators();
    Indicators getIndicators(EnumSet<StandardIndicator> indicatorNames, Problem problem, NondominatedPopulation referenceSet);
    Indicators getIndicators(Set<String> indicatorNames, Problem problem, NondominatedPopulation referenceSet);
}