package pl.edu.agh.to.kotospring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pl.edu.agh.to.kotospring.shared.TestModel;

@SpringBootApplication
public class Server {

    public static void main(String[] args) {
        TestModel testModel = new TestModel("test");
        SpringApplication.run(Server.class, args);
    }

}
