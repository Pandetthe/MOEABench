package pl.edu.agh.to.kotospring.client.shell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequest;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.CreateExperimentRequestData;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ShellComponent
public class ExperimentCommands {

    private final ExperimentClient experimentApi;

    public ExperimentCommands(ExperimentClient experimentApi) {
        this.experimentApi = experimentApi;
    }

    @ShellMethod(value = "Run experiment", key = "experiment-run")
    public Object experimentRun(
            @ShellOption String algorithms,
            @ShellOption String problems,
            @ShellOption String indicators,
            @ShellOption int budget
    ) {
        List<String> algorithmList = List.of(algorithms.split(","));
        List<String> problemList = List.of(problems.split(","));
        Set<String> indicatorSet = Set.of(indicators.split(","));

        CreateExperimentRequest request = new CreateExperimentRequest();

        for (String algorithm : algorithmList) {
            for (String problem : problemList) {

                Map<String, Object> algorithmParameters = Map.of();

                CreateExperimentRequestData data =
                        new CreateExperimentRequestData(
                                problem,
                                algorithm,
                                algorithmParameters,
                                indicatorSet,
                                budget
                        );

                request.add(data);
            }
        }

        return experimentApi.createExperiment(request);
    }

    @ShellMethod(value = "List experiments", key = "experiment-list")
    public Object experimentList() {
        return experimentApi.getExperiments();
    }

    @ShellMethod(value = "Get experiment status", key = "experiment-status")
    public Object experimentStatus(@ShellOption long id) {
        return experimentApi.getExperimentStatus(id);
    }
    @ShellMethod(value = "Show experiment part status", key = "experiment-part-status")
    public Object experimentPartStatus(
            @ShellOption long id,
            @ShellOption long part
    ) {
        return experimentApi.getExperimentPartStatus(id, part);
    }

    @ShellMethod(value = "Show full experiment", key = "experiment-show")
    public Object experimentShow(@ShellOption long id) {
        return experimentApi.getExperiment(id);
    }

    @ShellMethod(value = "Show experiment results", key = "experiment-results")
    public Object experimentResults(@ShellOption long id) {
        return experimentApi.getExperimentResult(id);
    }
    @ShellMethod(value = "Show experiment part results", key = "experiment-part-results")
    public Object experimentPartResults(
            @ShellOption long id,
            @ShellOption long part
    ) {
        return experimentApi.getExperimentPartResult(id, part);
    }

    @ShellMethod(value = "Watch experiment status", key = "experiment-watch")
    public void experimentWatch(@ShellOption long id) throws InterruptedException {
        while (true) {
            var status = experimentApi.getExperimentStatus(id);
            System.out.println(status);

            switch (status.status()) {
                case SUCCESS, PARTIAL_SUCCESS, FAILED -> {
                    return;
                }
            }


        }
    }
    @ShellMethod(value = "Delete experiment", key = "experiment-delete")
    public Object experimentDelete(@ShellOption long id) {
        experimentApi.deleteExperiment(id);
        return "Experiment Deleted";
    }
}
