package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.ArrayList;
import java.util.Collection;

public class GetExperimentRunResultResponse extends ArrayList<GetExperimentRunResultResponseData> {
    public GetExperimentRunResultResponse() {
        super();
    }

    public GetExperimentRunResultResponse(Collection<? extends GetExperimentRunResultResponseData> c) {
        super(c);
    }
}