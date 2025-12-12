package pl.edu.agh.to.kotospring.server.configuration;

import org.moeaframework.algorithm.DefaultAlgorithms;
import org.moeaframework.core.spi.RegisteredAlgorithmProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlgorithmProvidersConfiguration {

    @Bean
    public RegisteredAlgorithmProvider getDefaultAlgorithms() {
        return new DefaultAlgorithms();
    }
}
