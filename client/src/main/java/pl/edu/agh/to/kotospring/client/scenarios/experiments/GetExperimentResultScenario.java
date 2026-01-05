package pl.edu.agh.to.kotospring.client.scenarios.experiments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.shell.component.view.control.View;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.edu.agh.to.kotospring.client.api.ExperimentClient;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.Scenario;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioComponent;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioContext;
import pl.edu.agh.to.kotospring.client.scenarios.abstractions.ScenarioType;
import pl.edu.agh.to.kotospring.client.views.InputForm;
import pl.edu.agh.to.kotospring.client.views.SimpleMessageView;
import pl.edu.agh.to.kotospring.client.views.SimpleTableView;
import pl.edu.agh.to.kotospring.shared.experiments.AlgorithmResult;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentPartStatus;
import pl.edu.agh.to.kotospring.shared.experiments.ExperimentStatus;
import pl.edu.agh.to.kotospring.shared.experiments.contracts.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@ScenarioComponent(name = "Check result", type = ScenarioType.EXPERIMENT_MENU)
public class GetExperimentResultScenario extends Scenario {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final ExperimentClient client;
    private InputForm inputForm;

    public GetExperimentResultScenario(ExperimentClient client) {
        this.client = client;
    }

    @Override
    public View build() {
        inputForm = new InputForm(getTerminalUI(), "Check experiment (part) result");
        inputForm.addInput("id", "ID");
        inputForm.addInput("partId", "Part ID (optional)");
        inputForm.setSubmitAction("Check", this::handleCheckAction);

        configure(inputForm);
        inputForm.focusFirstInput();
        return inputForm;
    }

    private void handleCheckAction(Map<String, String> data) {
        View resultView;
        try {
            long id = Long.parseLong(data.get("id"));
            String partIdRaw = data.getOrDefault("partId", "");
            Optional<Long> partId = partIdRaw.isBlank()
                    ? Optional.empty()
                    : Optional.of(Long.parseLong(partIdRaw));

            if (partId.isPresent()) {
                resultView = handlePartResult(id, partId.get());
            } else {
                resultView = handleWholeExperimentResult(id);
            }

        } catch (RestClientResponseException e) {
            resultView = httpErrorView(e.getRawStatusCode(), e.getStatusText(), e.getResponseBodyAsString());
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            resultView = httpErrorView(e.getRawStatusCode(), e.getStatusText(), body);
        } catch (NumberFormatException e) {
            resultView = new SimpleMessageView("Invalid input", "ID and Part ID must be valid integers.");
        } catch (Exception e) {
            resultView = new SimpleMessageView(
                    "Error",
                    "Unexpected error: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())
            );
        }

        configure(resultView);
        if (resultView instanceof SimpleMessageView messageView) {
            configure(messageView.getContentList());
        }

        View finalResultView = resultView;
        navigate(ScenarioContext.of(resultView, () -> {
            if (finalResultView instanceof SimpleMessageView messageView) {
                getTerminalUI().setFocus(messageView.getContentList());
            } else {
                getTerminalUI().setFocus(finalResultView);
            }
        }, null));
    }

    private View handlePartResult(long id, long partId) {
        GetExperimentPartStatusResponse st = client.getExperimentPartStatus(id, partId);
        ExperimentPartStatus ps = st.status();

        if (ps == ExperimentPartStatus.FAILED) {
            return new SimpleMessageView(
                    "Result not available",
                    "Part " + partId + " FAILED.\n" +
                            (st.errorMessage() == null ? "No error message." : st.errorMessage())
            );
        }
        if (ps != ExperimentPartStatus.COMPLETED) {
            return new SimpleMessageView(
                    "Result not ready",
                    "Part " + partId + " is not completed yet. Status: " + ps
            );
        }

        GetExperimentPartResultResponse resp = client.getExperimentPartResult(id, partId);
        return partResultAsOneTableIndicatorsLast(partId, resp);
    }

    private View handleWholeExperimentResult(long id) {
        GetExperimentStatusResponse st = client.getExperimentStatus(id);
        ExperimentStatus es = st.status();

        if (es == ExperimentStatus.FAILED) {
            return new SimpleMessageView(
                    "Result not available",
                    "Experiment id=" + id + " FAILED.\n"
            );
        }
        if (!(es == ExperimentStatus.SUCCESS || es == ExperimentStatus.PARTIAL_SUCCESS)) {
            return new SimpleMessageView(
                    "Result not ready",
                    "Experiment id=" + id + " is not finished yet. Status: " + es
            );
        }

        GetExperimentResultResponse resp = client.getExperimentResult(id);
        return wholeExperimentAsOneTableIndicatorsLast(resp);
    }

    // --- Helpers (bez zmian logiki, tylko dostosowanie do klasy Scenario) ---

    private View httpErrorView(int status, String statusText, String body) {
        String msg = extractServerMessage(body);
        String title = "HTTP " + status + (statusText == null || statusText.isBlank() ? "" : " " + statusText);
        if (msg == null || msg.isBlank()) {
            msg = "Request failed (no details returned by server).";
        }
        return new SimpleMessageView(title, msg);
    }

    private String extractServerMessage(String body) {
        if (body == null) return "";
        String trimmed = body.trim();
        if (trimmed.isEmpty()) return "";

        if (!(trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            return trimmed;
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(trimmed);
            if (node.hasNonNull("message")) return node.get("message").asText();
            if (node.hasNonNull("error")) return node.get("error").asText();
            if (node.hasNonNull("detail")) return node.get("detail").asText();
            return trimmed;
        } catch (Exception ignored) {
            return trimmed;
        }
    }

    private View wholeExperimentAsOneTableIndicatorsLast(GetExperimentResultResponse resp) {
        AlgorithmResult firstSol = firstSolution(resp);

        List<String> varKeys = firstSol == null ? List.of() : keys(firstSol.variables());
        List<String> objKeys = firstSol == null ? List.of() : keys(firstSol.objectives());
        List<String> conKeys = firstSol == null ? List.of() : keys(firstSol.constraints());

        List<String> headers = new ArrayList<>();
        headers.add("Part ID");
        headers.addAll(varKeys);
        headers.addAll(objKeys);
        headers.addAll(conKeys);
        headers.add("Indicators");

        if (headers.size() == 2 && "Indicators".equals(headers.get(1))) {
            headers.add(1, "Solutions");
        }

        List<List<String>> rows = new ArrayList<>();
        boolean any = false;

        for (GetExperimentResultResponseData part : resp) {
            long partId = part.id();
            String indicatorsCompact = compactIndicators(part.indicatorsValues());

            List<AlgorithmResult> results = part.result();
            if (results == null || results.isEmpty()) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(partId));
                row.add("No solutions");
                int numericCols = varKeys.size() + objKeys.size() + conKeys.size();
                for (int i = 1; i < numericCols; i++) row.add("-");
                row.add(indicatorsCompact);
                rows.add(row);
                any = true;
                continue;
            }

            any = true;
            boolean fallbackToString = headers.contains("Solutions");

            for (AlgorithmResult r : results) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(partId));

                if (fallbackToString) {
                    row.add(String.valueOf(r));
                    int numericCols = varKeys.size() + objKeys.size() + conKeys.size();
                    for (int i = 1; i < numericCols; i++) row.add("-");
                } else {
                    for (String k : varKeys) row.add(formatAny(r.variables() == null ? null : r.variables().get(k)));
                    for (String k : objKeys) row.add(formatDouble(r.objectives() == null ? null : r.objectives().get(k)));
                    for (String k : conKeys) row.add(formatDouble(r.constraints() == null ? null : r.constraints().get(k)));
                }

                row.add(indicatorsCompact);
                rows.add(row);
            }
        }

        if (!any) {
            return new SimpleMessageView("Results", "No results returned.");
        }

        List<Integer> colWidths = widths(headers, rows);
        SimpleTableView table = new SimpleTableView(headers, rows, colWidths);

        table.enableColumnPaging(1, 7);
        table.setShowFullNumericCells(true);
        table.setIndicatorsFullOnLastPage(true);
        table.setIndicatorsLastPageWidth(30);

        return table;
    }

    private View partResultAsOneTableIndicatorsLast(long partId, GetExperimentPartResultResponse resp) {
        List<AlgorithmResult> results = resp.result();
        AlgorithmResult first = (results == null || results.isEmpty()) ? null : results.get(0);

        List<String> varKeys = first == null ? List.of() : keys(first.variables());
        List<String> objKeys = first == null ? List.of() : keys(first.objectives());
        List<String> conKeys = first == null ? List.of() : keys(first.constraints());

        List<String> headers = new ArrayList<>();
        headers.add("Part ID");
        headers.addAll(varKeys);
        headers.addAll(objKeys);
        headers.addAll(conKeys);
        headers.add("Indicators");

        if (headers.size() == 2 && "Indicators".equals(headers.get(1))) {
            headers.add(1, "Solutions");
        }

        String indicatorsCompact = compactIndicators(resp.indicatorsValues());

        List<List<String>> rows = new ArrayList<>();
        if (results == null || results.isEmpty()) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(partId));
            row.add("No solutions");
            int numericCols = varKeys.size() + objKeys.size() + conKeys.size();
            for (int i = 1; i < numericCols; i++) row.add("-");
            row.add(indicatorsCompact);
            rows.add(row);
        } else {
            boolean fallbackToString = headers.contains("Solutions");
            for (AlgorithmResult r : results) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(partId));

                if (fallbackToString) {
                    row.add(String.valueOf(r));
                    int numericCols = varKeys.size() + objKeys.size() + conKeys.size();
                    for (int i = 1; i < numericCols; i++) row.add("-");
                } else {
                    for (String k : varKeys) row.add(formatAny(r.variables() == null ? null : r.variables().get(k)));
                    for (String k : objKeys) row.add(formatDouble(r.objectives() == null ? null : r.objectives().get(k)));
                    for (String k : conKeys) row.add(formatDouble(r.constraints() == null ? null : r.constraints().get(k)));
                }

                row.add(indicatorsCompact);
                rows.add(row);
            }
        }

        List<Integer> colWidths = widths(headers, rows);
        SimpleTableView table = new SimpleTableView(headers, rows, colWidths);
        table.enableColumnPaging(1, 7);
        table.setShowFullNumericCells(true);
        table.setIndicatorsFullOnLastPage(true);
        table.setIndicatorsLastPageWidth(30);

        return table;
    }

    private static AlgorithmResult firstSolution(GetExperimentResultResponse resp) {
        for (GetExperimentResultResponseData part : resp) {
            List<AlgorithmResult> results = part.result();
            if (results != null && !results.isEmpty()) return results.get(0);
        }
        return null;
    }

    private static String compactIndicators(Map<String, Double> indicators) {
        if (indicators == null || indicators.isEmpty()) return "-";
        return indicators.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + formatDoubleShort(e.getValue()))
                .collect(Collectors.joining("; "));
    }

    private static <T> List<String> keys(Map<String, T> map) {
        return (map == null || map.isEmpty()) ? List.of() : new ArrayList<>(map.keySet());
    }

    private static String formatAny(Object v) {
        if (v == null) return "-";
        if (v instanceof Double d) return formatDoubleShort(d);
        if (v instanceof Float f) return formatDoubleShort(f.doubleValue());
        if (v instanceof Number n) return formatDoubleShort(n.doubleValue());
        return String.valueOf(v);
    }

    private static String formatDouble(Double v) {
        if (v == null) return "-";
        return formatDoubleShort(v);
    }

    private static String formatDoubleShort(Double v) {
        if (v == null) return "-";
        double x = v;
        double ax = Math.abs(x);

        if (ax != 0.0 && (ax < 1e-3 || ax >= 1e4)) {
            return String.format(Locale.ROOT, "%.3e", x)
                    .replace("e+0", "e+")
                    .replace("e-0", "e-");
        }

        String s = String.format(Locale.ROOT, "%.4f", x);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s.isEmpty() ? "0" : s;
    }

    private static List<Integer> widths(List<String> headers, List<List<String>> rows) {
        int cols = headers.size();
        int[] w = new int[cols];

        for (int i = 0; i < cols; i++) w[i] = headers.get(i) == null ? 0 : headers.get(i).length();
        for (List<String> r : rows) {
            for (int i = 0; i < cols; i++) {
                String cell = (r != null && i < r.size() && r.get(i) != null) ? r.get(i) : "";
                w[i] = Math.max(w[i], cell.length());
            }
        }

        List<Integer> out = new ArrayList<>(cols);
        for (int i = 0; i < cols; i++) {
            String h = headers.get(i);

            int minW = Math.max(5, h == null ? 5 : h.length());
            int maxW;

            if ("Part ID".equals(h)) maxW = 6;
            else if ("Indicators".equals(h)) maxW = 18;
            else if ("Solutions".equals(h)) maxW = 30;
            else maxW = 12;

            out.add(Math.min(maxW, Math.max(minW, w[i])));
        }
        return out;
    }
}