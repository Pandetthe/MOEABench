package pl.edu.agh.to.kotospring.server.indicators;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.edu.agh.to.kotospring.server.indicators.providers.DefaultIndicators;
import pl.edu.agh.to.kotospring.server.indicators.providers.RegisteredIndicatorProvider;

@Configuration
public class IndicatorProvidersConfiguration {

    @Bean
    public RegisteredIndicatorProvider getDefaultIndicators() {
        return new DefaultIndicators();
    }
}
