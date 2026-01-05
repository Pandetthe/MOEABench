package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.ArrayList;
import java.util.Collection;

public class GetExperimentResultResponse extends ArrayList<GetExperimentResultResponseData> {
    public GetExperimentResultResponse() {
        super();
    }

    public GetExperimentResultResponse(Collection<? extends GetExperimentResultResponseData> c) {
        super(c);
    }
}
