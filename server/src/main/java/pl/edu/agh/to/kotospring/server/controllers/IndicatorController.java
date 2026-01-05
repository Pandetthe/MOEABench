package pl.edu.agh.to.kotospring.server.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.to.kotospring.server.services.implementation.IndicatorRegistryServiceImpl;

import java.util.Set;

@RestController
@RequestMapping(path = "/indicators")
public class IndicatorController {
    private final IndicatorRegistryServiceImpl indicatorRegistryService;

    public IndicatorController(IndicatorRegistryServiceImpl indicatorRegistryService) {
        this.indicatorRegistryService = indicatorRegistryService;
    }

    @GetMapping
    public Set<String> getIndicators() {
        return indicatorRegistryService.getAllRegisteredIndicators();
    }
}
