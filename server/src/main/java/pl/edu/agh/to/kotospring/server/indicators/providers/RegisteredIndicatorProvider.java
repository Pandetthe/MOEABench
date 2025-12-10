package pl.edu.agh.to.kotospring.server.indicators.providers;

import org.moeaframework.core.Settings;

import java.util.*;

public class RegisteredIndicatorProvider extends IndicatorProvider {
    private final Set<String> constructorMap;

    public RegisteredIndicatorProvider() {
        super();
        constructorMap = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    }

    public Set<String> getRegisteredIndicators() {
        return Collections.unmodifiableSet(constructorMap);
    }

    protected final void register(String name) {
        if (constructorMap.contains(name) && Settings.isVerbose()) {
            System.err.println("WARNING: Previously registered indicator '" + name + "' is being redefined by " +
                    getClass().getSimpleName());
        }

        constructorMap.add(name);
    }
}
