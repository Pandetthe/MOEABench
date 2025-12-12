package pl.edu.agh.to.kotospring.server.exceptions;

import java.util.Set;

public class IndicatorNotFoundException extends NotFoundException {
    public IndicatorNotFoundException(Set<String> indicatorNames) {
        super("Indicators not found: " + String.join(", ", indicatorNames));
    }
}