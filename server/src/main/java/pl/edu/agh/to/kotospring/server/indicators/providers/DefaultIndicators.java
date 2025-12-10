package pl.edu.agh.to.kotospring.server.indicators.providers;

public class DefaultIndicators extends RegisteredIndicatorProvider {
    public DefaultIndicators() {
        super();
        register("Hypervolume");
        register("GenerationalDistance");
        register("GenerationalDistancePlus");
        register("InvertedGenerationalDistance");
        register("InvertedGenerationalDistancePlus");
        register("AdditiveEpsilonIndicator");
        register("MaximumParetoFrontError");
        register("Spacing");
        register("Contribution");
        register("R1Indicator");
        register("R2Indicator");
        register("R3Indicator");
    }
}
