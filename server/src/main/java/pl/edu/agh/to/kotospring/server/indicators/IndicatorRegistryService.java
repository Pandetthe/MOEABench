package pl.edu.agh.to.kotospring.server.indicators;

import org.moeaframework.core.spi.RegisteredProblemProvider;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.kotospring.server.indicators.providers.RegisteredIndicatorProvider;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
public class IndicatorRegistryService {
    private final List<RegisteredIndicatorProvider> providers;

    public IndicatorRegistryService(List<RegisteredIndicatorProvider> providers) {
        this.providers = providers;
    }

    public synchronized Set<String> getAllRegisteredIndicators() {
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (RegisteredIndicatorProvider provider : providers) {
            result.addAll(provider.getRegisteredIndicators());
        }
        return result;
    }
}
