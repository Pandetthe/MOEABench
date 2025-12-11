package pl.edu.agh.to.kotospring.server.problems;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.spi.ProviderNotFoundException;
import org.moeaframework.core.spi.RegisteredProblemProvider;
import org.moeaframework.problem.Problem;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProblemRegistryServiceTest {

    @Mock
    private RegisteredProblemProvider provider1;

    @Mock
    private RegisteredProblemProvider provider2;

    @Mock
    private Problem problem;

    @Mock
    private NondominatedPopulation referenceSet;

    private ProblemRegistryService service;

    @BeforeEach
    void setUp() {
        service = new ProblemRegistryService(Arrays.asList(provider1, provider2));
    }

    @Test
    void getAllRegisteredProblems_combinesAndSortsCaseInsensitive() {
        when(provider1.getRegisteredProblems()).thenReturn(new HashSet<>(Arrays.asList("bProblem", "aProblem")));
        when(provider2.getRegisteredProblems()).thenReturn(new HashSet<>(Arrays.asList("CProblem", "aProblem2")));

        Set<String> result = service.getAllRegisteredProblems();

        List<String> actual = new ArrayList<>(result);
        List<String> expected = Arrays.asList("aProblem", "aProblem2", "bProblem", "CProblem");
        assertEquals(expected, actual);
    }

    @Test
    void getProblem_nullOrEmptyName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.getProblem(null));
        assertThrows(IllegalArgumentException.class, () -> service.getProblem(""));
    }

    @Test
    void getProblem_returnsProblemFromProvider() {
        String name = "testProblem";

        when(provider1.getProblem(eq(name))).thenReturn(problem);

        Problem result = service.getProblem(name);

        assertSame(problem, result);
        verify(provider1, times(1)).getProblem(eq(name));
        verifyNoInteractions(provider2);
    }

    @Test
    void getProblem_skipsProviderOnServiceConfigurationError_andUsesNext() {
        String name = "testProblem";

        when(provider1.getProblem(eq(name))).thenThrow(new java.util.ServiceConfigurationError("broken"));
        when(provider2.getProblem(eq(name))).thenReturn(problem);

        Problem result = service.getProblem(name);

        assertSame(problem, result);
        verify(provider1, times(1)).getProblem(eq(name));
        verify(provider2, times(1)).getProblem(eq(name));
    }

    @Test
    void getProblem_notFound_throwsProviderNotFoundException() {
        String name = "missingProblem";

        when(provider1.getProblem(eq(name))).thenReturn(null);
        when(provider2.getProblem(eq(name))).thenReturn(null);

        assertThrows(ProviderNotFoundException.class, () -> service.getProblem(name));
    }

    @Test
    void getReferenceSet_nullOrEmptyName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.getReferenceSet(null));
        assertThrows(IllegalArgumentException.class, () -> service.getReferenceSet(""));
    }

    @Test
    void getReferenceSet_returnsReferenceSetFromProvider() {
        String name = "refProblem";

        when(provider1.getReferenceSet(eq(name))).thenReturn(referenceSet);

        NondominatedPopulation result = service.getReferenceSet(name);

        assertSame(referenceSet, result);
        verify(provider1, times(1)).getReferenceSet(eq(name));
        verifyNoInteractions(provider2);
    }

    @Test
    void getReferenceSet_skipsProviderOnServiceConfigurationError_andUsesNext() {
        String name = "refProblem";

        when(provider1.getReferenceSet(eq(name))).thenThrow(new java.util.ServiceConfigurationError("broken"));
        when(provider2.getReferenceSet(eq(name))).thenReturn(referenceSet);

        NondominatedPopulation result = service.getReferenceSet(name);

        assertSame(referenceSet, result);
        verify(provider1, times(1)).getReferenceSet(eq(name));
        verify(provider2, times(1)).getReferenceSet(eq(name));
    }

    @Test
    void getReferenceSet_notFound_throwsProviderNotFoundException() {
        String name = "missingRef";

        when(provider1.getReferenceSet(eq(name))).thenReturn(null);
        when(provider2.getReferenceSet(eq(name))).thenReturn(null);

        assertThrows(ProviderNotFoundException.class, () -> service.getReferenceSet(name));
    }

    @Test
    void getProblem_isCaseSensitive_whenProviderHandlesCaseSensitively() {
        String registeredName = "MyProblem";
        String requestedDifferentCase = "myproblem";
        String requestedExact = "MyProblem";

        doAnswer(invocation -> {
            String nameArg = invocation.getArgument(0);
            if (registeredName.equals(nameArg)) {
                return problem;
            }
            return null;
        }).when(provider1).getProblem(anyString());

        assertThrows(ProviderNotFoundException.class, () -> service.getProblem(requestedDifferentCase));
        verify(provider1, times(1)).getProblem(eq(requestedDifferentCase));

        Problem found = service.getProblem(requestedExact);
        assertSame(problem, found);
        verify(provider1, times(1)).getProblem(eq(requestedExact));
    }

    @Test
    void getProblem_isCaseInsensitive_whenProviderHandlesCaseInsensitively() {
        String registeredName = "MyProblem";
        String requestedName = "myproblem";

        doAnswer(invocation -> {
            String nameArg = invocation.getArgument(0);
            if (registeredName.equalsIgnoreCase(nameArg)) {
                return problem;
            }
            return null;
        }).when(provider1).getProblem(anyString());

        Problem result = service.getProblem(requestedName);

        assertSame(problem, result);
        verify(provider1, times(1)).getProblem(eq(requestedName));
    }
}