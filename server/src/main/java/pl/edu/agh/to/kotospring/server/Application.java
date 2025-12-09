package pl.edu.agh.to.kotospring.server;

import org.moeaframework.algorithm.NSGAII;
import org.moeaframework.core.TypedProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
