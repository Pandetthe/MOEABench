package pl.edu.agh.to.kotospring.server.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class SpringAsyncConfig {
    private static final Logger logger = LoggerFactory.getLogger(SpringAsyncConfig.class);

    @Bean(name = "experimentExecutor")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("ExperimentExecutor-");
        executor.setRejectedExecutionHandler((task, pool) ->
                logger.error("Experiment task rejected — executor queue full (queue={}, active={}). " +
                        "The affected part will remain QUEUED in the database.",
                        pool.getQueue().size(), pool.getActiveCount()));
        executor.initialize();
        return executor;
    }
}
