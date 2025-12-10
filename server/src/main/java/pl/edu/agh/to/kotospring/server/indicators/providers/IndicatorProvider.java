package pl.edu.agh.to.kotospring.server.indicators.providers;

import java.util.HashSet;
import java.util.Set;

public class IndicatorProvider {
    public IndicatorProvider() {
        super();
    }

    public Set<String> getDiagnosticToolIndicators() {
        return new HashSet<>();
    }


    //public abstract Indicator getIndicator(String name);
}
