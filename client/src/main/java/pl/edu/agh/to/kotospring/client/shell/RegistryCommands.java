package pl.edu.agh.to.kotospring.client.shell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import pl.edu.agh.to.kotospring.client.api.RegistryClient;

import java.util.List;

@ShellComponent
public class RegistryCommands {

    private final RegistryClient api;

    public RegistryCommands(RegistryClient api) {
        this.api = api;
    }

    @ShellMethod("List available algorithms")
    public List<String> algorithms() {
        return api.getAlgorithms();
    }

    @ShellMethod("List available problems")
    public List<String> problems() {
        return api.getProblems();
    }

    @ShellMethod("List available indicators")
    public List<String> indicators() {
        return api.getIndicators();
    }
}
