package pl.edu.agh.to.kotospring.client.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class TestCommand {
    @ShellMethod(key = "test", value = "A test command to verify the shell is working.")
    public String testCommand() {
        return "Test command executed successfully!";
    }
}
