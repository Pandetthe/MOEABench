package pl.edu.agh.to.kotospring.server.algorithms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moeaframework.algorithm.Algorithm;
import org.moeaframework.core.TypedProperties;
import org.moeaframework.core.spi.ProviderNotFoundException;
import org.moeaframework.core.spi.RegisteredAlgorithmProvider;
import org.moeaframework.problem.Problem;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.agh.to.kotospring.server.services.implementation.AlgorithmRegistryServiceImpl;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlgorithmRegistryServiceTest {

    @Mock
    private RegisteredAlgorithmProvider provider1;

    @Mock
    private RegisteredAlgorithmProvider provider2;

    @Mock
    private Algorithm algorithm;

    @Mock
    private Problem problem;

    private AlgorithmRegistryServiceImpl service;

    @Captor
    private ArgumentCaptor<TypedProperties> typedPropertiesCaptor;

    @BeforeEach
    void setUp() {
        service = new AlgorithmRegistryServiceImpl(Arrays.asList(provider1, provider2));
    }

    @Test
    void getAllRegisteredAlgorithms_combinesAndSortsCaseInsensitive() {
        when(provider1.getRegisteredAlgorithms()).thenReturn(Set.of("bAlgo", "aAlgo"));
        when(provider2.getRegisteredAlgorithms()).thenReturn(Set.of("CAlgo", "aAlgo2"));

        Set<String> result = service.getAllRegisteredAlgorithms();

        List<String> actual = new ArrayList<>(result);
        List<String> expected = Arrays.asList("aAlgo", "aAlgo2", "bAlgo", "CAlgo");
        assertEquals(expected, actual);
    }

    @Test
    void getAlgorithm_nullName_throwsIllegalArgumentException() {
        TypedProperties props = new TypedProperties();
        assertThrows(IllegalArgumentException.class, () -> service.getAlgorithm(null, props, problem));
        assertThrows(IllegalArgumentException.class, () -> service.getAlgorithm("", props, problem));
    }

    @Test
    void getAlgorithm_returnsAlgorithmFromProvider() {
        String name = "testAlgo";
        TypedProperties props = new TypedProperties();

        when(provider1.getAlgorithm(eq(name), eq(props), eq(problem))).thenReturn(algorithm);

        Algorithm result = service.getAlgorithm(name, props, problem);

        assertSame(algorithm, result);
        verify(provider1, times(1)).getAlgorithm(eq(name), eq(props), eq(problem));
        verifyNoInteractions(provider2);
    }

    @Test
    void getAlgorithm_skipsProviderOnServiceConfigurationError_andUsesNext() {
        String name = "testAlgo";
        TypedProperties props = new TypedProperties();

        when(provider1.getAlgorithm(eq(name), eq(props), eq(problem)))
                .thenThrow(new java.util.ServiceConfigurationError("broken provider"));
        when(provider2.getAlgorithm(eq(name), eq(props), eq(problem))).thenReturn(algorithm);

        Algorithm result = service.getAlgorithm(name, props, problem);

        assertSame(algorithm, result);
        verify(provider1, times(1)).getAlgorithm(eq(name), eq(props), eq(problem));
        verify(provider2, times(1)).getAlgorithm(eq(name), eq(props), eq(problem));
    }

    @Test
    void getAlgorithm_notFound_throwsProviderNotFoundException() {
        String name = "missingAlgo";
        TypedProperties props = new TypedProperties();

        when(provider1.getAlgorithm(eq(name), any(TypedProperties.class), eq(problem))).thenReturn(null);
        when(provider2.getAlgorithm(eq(name), any(TypedProperties.class), eq(problem))).thenReturn(null);

        assertThrows(ProviderNotFoundException.class, () -> service.getAlgorithm(name, props, problem));
    }

    @Test
    void getAlgorithm_withProperties_delegatesAndConvertsToTypedProperties() {
        String name = "propAlgo";
        Properties properties = new Properties();
        properties.setProperty("pKey", "pValue");

        when(provider1.getAlgorithm(eq(name), any(TypedProperties.class), eq(problem))).thenReturn(algorithm);

        Algorithm result = service.getAlgorithm(name, properties, problem);

        assertSame(algorithm, result);
        verify(provider1, times(1)).getAlgorithm(eq(name), typedPropertiesCaptor.capture(), eq(problem));

        TypedProperties passed = typedPropertiesCaptor.getValue();
        assertEquals("pValue", passed.getString("pKey"));
    }

    @Test
    void getAlgorithm_isCaseInsensitive_whenProviderHandlesCaseInsensitively() {
        String registeredName = "MyAlgo";
        String requestedName = "myalgo";
        TypedProperties props = new TypedProperties();

        doAnswer(invocation -> {
            String nameArg = invocation.getArgument(0);
            if (registeredName.equalsIgnoreCase(nameArg)) {
                return algorithm;
            }
            return null;
        }).when(provider1).getAlgorithm(anyString(), any(TypedProperties.class), eq(problem));

        Algorithm result = service.getAlgorithm(requestedName, props, problem);

        assertSame(algorithm, result);
        verify(provider1, times(1)).getAlgorithm(eq(requestedName), eq(props), eq(problem));
    }

    @Test
    void getAlgorithm_isCaseSensitive_whenProviderHandlesCaseSensitively() {
        String registeredName = "MyAlgo";
        String requestedName = "myalgo";
        TypedProperties props = new TypedProperties();

        doAnswer(invocation -> {
            String nameArg = invocation.getArgument(0);
            if (registeredName.equals(nameArg)) {
                return algorithm;
            }
            return null;
        }).when(provider1).getAlgorithm(anyString(), any(TypedProperties.class), eq(problem));

        assertThrows(ProviderNotFoundException.class, () -> service.getAlgorithm(requestedName, props, problem));
        verify(provider1, times(1)).getAlgorithm(eq(requestedName), eq(props), eq(problem));

        Algorithm found = service.getAlgorithm(registeredName, props, problem);
        assertSame(algorithm, found);
        verify(provider1, times(1)).getAlgorithm(eq(registeredName), eq(props), eq(problem));
    }
}