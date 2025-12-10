package pl.edu.agh.to.kotospring.server.algorithms;

import org.moeaframework.core.spi.AbstractFactory;
import org.moeaframework.core.spi.AlgorithmProvider;
import org.moeaframework.core.spi.RegisteredAlgorithmProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
public class AlgorithmRegistryService {
    private final List<RegisteredAlgorithmProvider> providers;

    public AlgorithmRegistryService(List<RegisteredAlgorithmProvider> providers) {
        this.providers = providers;
    }

    public synchronized Set<String> getAllRegisteredAlgorithms() {
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (RegisteredAlgorithmProvider provider : providers) {
            result.addAll(provider.getRegisteredAlgorithms());
        }
        return result;
    }
}
