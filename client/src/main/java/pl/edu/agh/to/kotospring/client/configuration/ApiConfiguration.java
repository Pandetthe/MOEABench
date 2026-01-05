package pl.edu.agh.to.kotospring.client.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.api.RegistryClient;
import pl.edu.agh.to.kotospring.client.properties.ServerClientProperties;

@Configuration
public class ApiConfiguration {
    @Bean
    public ExperimentClient experimentClient(WebClient.Builder webClientBuilder,
                                         ServerClientProperties props) {
        WebClient webClient = webClientBuilder
                .baseUrl(props.getBaseUrl())
                .build();

        WebClientAdapter adapter = WebClientAdapter.create(webClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(ExperimentClient.class);
    }

    @Bean
    public RegistryClient registryClient(WebClient.Builder webClientBuilder,
                                       ServerClientProperties props) {
        WebClient webClient = webClientBuilder
                .baseUrl(props.getBaseUrl())
                .build();

        WebClientAdapter adapter = WebClientAdapter.create(webClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(RegistryClient.class);
    }
}
