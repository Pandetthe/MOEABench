package pl.edu.agh.to.kotospring.server.indicators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.Solution;
import org.moeaframework.core.indicator.Indicators;
import org.moeaframework.core.indicator.StandardIndicator;
import org.moeaframework.core.objective.Objective;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.AbstractProblem;
import org.moeaframework.problem.Problem;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class IndicatorRegistryServiceTest {

    private IndicatorRegistryService service;

    private Problem problem;
    private NondominatedPopulation referenceSet;

    @BeforeEach
    void setUp() {
        service = new IndicatorRegistryService();
        problem = new DummyProblem(0, 2);
        referenceSet = new NondominatedPopulation();
        Solution s1 = problem.newSolution();
        Solution s2 = problem.newSolution();
        referenceSet.add(s1);
        referenceSet.add(s2);
    }

    @Test
    void getAllRegisteredIndicators_containsAllStandardIndicators_caseInsensitiveSorted() {
        Set<String> actual = service.getAllRegisteredIndicators();
        Set<String> expectedSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (StandardIndicator si : StandardIndicator.values()) {
            expectedSet.add(si.name());
        }

        List<String> actualList = new ArrayList<>(actual);
        List<String> expectedList = new ArrayList<>(expectedSet);

        assertEquals(expectedList, actualList, "Registered indicators should match all StandardIndicator names in case-insensitive sorted order");
    }

    @Test
    void getIndicators_withEnumSet_nullThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.getIndicators((EnumSet<StandardIndicator>) null, problem, referenceSet));
    }

    @Test
    void getIndicators_withEnumSet_includesRequestedIndicators() {
        StandardIndicator first = StandardIndicator.values()[0];
        EnumSet<StandardIndicator> requested = EnumSet.of(first);

        Indicators indicators = service.getIndicators(requested, problem, referenceSet);

        assertNotNull(indicators, "Indicators instance should not be null");
        assertDoesNotThrow(() -> {
            Object ind = indicators.getIndicator(first);
            assertNotNull(ind, "Requested indicator should be present");
        });
    }

    @Test
    void getIndicators_withStringSet_nullThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.getIndicators((Set<String>) null, problem, referenceSet));
    }

    @Test
    void getIndicators_withStringSet_convertsAndReturnsIndicators() {
        String validName = StandardIndicator.values()[0].name();
        Set<String> names = Collections.singleton(validName);

        Indicators indicators = service.getIndicators(names, problem, referenceSet);

        assertNotNull(indicators, "Indicators instance should not be null for valid indicator names");

        StandardIndicator si = StandardIndicator.valueOf(validName);
        assertDoesNotThrow(() -> {
            Object ind = indicators.getIndicator(si);
            assertNotNull(ind, "Converted indicator should be present");
        });
    }

    @Test
    void getIndicators_withStringSet_invalidNamesThrows_andListsInvalids() {
        Set<String> names = new HashSet<>(Arrays.asList("NOT_AN_INDICATOR", "ALSO_INVALID"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getIndicators(names, problem, referenceSet));

        String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("NOT_AN_INDICATOR"), "Exception message should include invalid indicator name");
        assertTrue(msg.contains("ALSO_INVALID"), "Exception message should include invalid indicator name");
    }

    static class DummyProblem extends AbstractProblem {
        DummyProblem(int numberOfVariables, int numberOfObjectives) {
            super(numberOfVariables, numberOfObjectives);
        }

        @Override
        public void evaluate(Solution solution) {
            for (int i = 0; i < getNumberOfObjectives(); i++) {
                solution.setObjective(i, Objective.createDefault());
            }
        }

        @Override
        public Solution newSolution() {
            return new Solution(getNumberOfVariables(), getNumberOfObjectives());
        }
    }
}