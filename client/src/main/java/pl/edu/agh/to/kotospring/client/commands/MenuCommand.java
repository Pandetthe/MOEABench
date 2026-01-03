package pl.edu.agh.to.kotospring.client.commands;

import java.util.List;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.component.view.TerminalUIBuilder;
import org.springframework.shell.standard.AbstractShellComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.services.MainMenu;

@Command
public class MenuCommand extends AbstractShellComponent {
    private final List<Scenario> scenarios;
    private final TerminalUIBuilder terminalUIBuilder;

    public MenuCommand(List<Scenario> scenarios, TerminalUIBuilder terminalUIBuilder) {
        this.scenarios = scenarios;
        this.terminalUIBuilder = terminalUIBuilder;
    }

    @Command(command = "menu")
    public void mainMenu() {
        MainMenu mainMenu = new MainMenu(terminalUIBuilder, scenarios);
        mainMenu.run();
    }
}