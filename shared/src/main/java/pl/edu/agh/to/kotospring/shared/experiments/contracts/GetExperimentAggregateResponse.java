package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.ArrayList;
import java.util.Collection;

public class GetExperimentAggregateResponse extends ArrayList<GetExperimentAggregateData> {
    public GetExperimentAggregateResponse() {
        super();
    }

    public GetExperimentAggregateResponse(Collection<? extends GetExperimentAggregateData> c) {
        super(c);
    }
}
