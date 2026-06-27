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
    public HttpServiceProxyFactory httpServiceProxyFactory(WebClient.Builder webClientBuilder,
            ServerClientProperties props) {
        WebClient webClient = webClientBuilder
                .baseUrl(props.getBaseUrl())
                .build();
        return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
    }

    @Bean
    public ExperimentClient experimentClient(HttpServiceProxyFactory factory) {
        return factory.createClient(ExperimentClient.class);
    }

    @Bean
    public RegistryClient registryClient(HttpServiceProxyFactory factory) {
        return factory.createClient(RegistryClient.class);
    }
}
