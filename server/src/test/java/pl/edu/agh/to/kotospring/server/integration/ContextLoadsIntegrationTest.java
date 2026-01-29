package pl.edu.agh.to.kotospring.server.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pl.edu.agh.to.kotospring.server.Application;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class ContextLoadsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void contextLoads() {
    }
}
