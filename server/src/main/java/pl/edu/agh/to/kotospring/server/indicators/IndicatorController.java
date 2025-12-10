package pl.edu.agh.to.kotospring.server.indicators;

import org.moeaframework.core.indicator.Indicators;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping(path = "/indicators")
public class IndicatorController {
    private final IndicatorRegistryService indicatorRegistryService;

    public IndicatorController(IndicatorRegistryService indicatorRegistryService) {
        this.indicatorRegistryService = indicatorRegistryService;
    }

    @GetMapping
    public Set<String> getIndicators() {
        return indicatorRegistryService.getAllRegisteredIndicators();
    }
}
