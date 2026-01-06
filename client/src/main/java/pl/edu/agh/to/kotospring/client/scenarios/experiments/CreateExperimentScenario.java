//package pl.edu.agh.to.kotospring.client.scenarios.experiments;
//
//import org.springframework.shell.component.view.control.View;
//import org.springframework.web.client.RestClientResponseException;
//import org.springframework.web.reactive.function.client.WebClientResponseException;
//import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
//import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
//import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
//import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioContext;
//import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
//import pl.edu.agh.to.kotospring.client.views.InputForm;
//import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
//import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;
//
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//@ScenarioComponent(name = "Create new Experiment", type = ScenarioType.EXPERIMENT_MENU, skipOnReturn = true)
//public class CreateExperimentScenario extends Scenario {
//
//    private final ExperimentClient client;
//    private final ExperimentErrorHandler errorHandler;
//
//    public CreateExperimentScenario(ExperimentClient client, ExperimentErrorHandler errorHandler) {
//        this.client = client;
//        this.errorHandler = errorHandler;
//    }
//
//    @Override
//    public View build() {
//        InputForm form = new InputForm(this.getTerminalUI(), "Create Experiment");
//        form.addInput("problems", "Problems (comma separated)");
//        form.addInput("algorithms", "Algorithms (comma separated)");
//        form.addInput("indicators", "Indicators (comma separated)");
//        form.addInput("budget", "Budget (integer)");
//        form.addInput("runsNo", "Number of runs (integer)");
//        form.setSubmitAction("Create", this::handleCreateAction);
//
//        configure(form);
//        form.focusFirstInput();
//        return form;
//    }
//
//    private void handleCreateAction(Map<String, String> data) {
//        View resultView;
//        try {
//            List<String> algorithmList = List.of(data.get("algorithms").split(","));
//            List<String> problemList = List.of(data.get("problems").split(","));
//            Set<String> indicatorSet = Set.of(data.get("indicators").split(","));
//            int budget = Integer.parseInt(data.get("budget"));
//            int runsNo = Integer.parseInt(data.get("runsNo"));
//            CreateExperimentFullRequest request = new CreateExperimentFullRequest();
//
//            for (String algorithm : algorithmList) {
//                if (algorithm.isBlank()) continue;
//                for (String problem : problemList) {
//                    if (problem.isBlank()) continue;
//                    Map<String, Object> algorithmParameters = Map.of();
//                    CreateExperimentFullRequestData requestData =
//                            new CreateExperimentFullRequestData(
//                                    problem.trim(),
//                                    algorithm.trim(),
//                                    algorithmParameters,
//                                    indicatorSet,
//                                    budget,
//                                    runsNo
//                            );
//                    request.add(requestData);
//                }
//            }
//
//            if (request.isEmpty()) {
//                throw new IllegalArgumentException("No valid experiments defined. Check inputs.");
//            }
//            CreateExperimentFullResponse response = client.createExperiment(request, runsNo);
//
//            resultView = new SimpleMessageView(
//                    "Success",
//                    "Experiment successfully created with ID: " + response.id()
//            );
//
//        } catch (RestClientResponseException e) {
//            resultView = errorHandler.httpErrorView(
//                    e.getRawStatusCode(),
//                    e.getStatusText(),
//                    e.getResponseBodyAsString()
//            );
//        } catch (WebClientResponseException e) {
//            String body = e.getResponseBodyAsString(StandardCharsets.UTF_8);
//            resultView = errorHandler.httpErrorView(e.getRawStatusCode(),
//                    e.getStatusText(),
//                    body);
//        } catch (NumberFormatException e) {
//            resultView = new SimpleMessageView(
//                    "Invalid input",
//                    "Budget must be a valid integer."
//            );
//        } catch (Exception e) {
//            resultView = new SimpleMessageView(
//                    "Error",
//                    "Unexpected error: " +
//                            (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())
//            );
//        }
//        configure(resultView);
//
//        View finalResultView = resultView;
//        navigate(ScenarioContext.of(resultView, null, () -> {
//            if (finalResultView instanceof SimpleMessageView mv) {
//                getTerminalUI().setFocus(mv.getContentList());
//            } else {
//                getTerminalUI().setFocus(finalResultView);
//            }
//        }, null));
//    }
//}