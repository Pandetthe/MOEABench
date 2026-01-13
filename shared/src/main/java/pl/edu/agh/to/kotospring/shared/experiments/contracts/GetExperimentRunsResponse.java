package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.ArrayList;
import java.util.Collection;

public class GetExperimentRunsResponse extends ArrayList<GetExperimentRunsResponseData> {
    public GetExperimentRunsResponse() {
        super();
    }

    public GetExperimentRunsResponse(Collection<? extends GetExperimentRunsResponseData> c) {
        super(c);
    }
}