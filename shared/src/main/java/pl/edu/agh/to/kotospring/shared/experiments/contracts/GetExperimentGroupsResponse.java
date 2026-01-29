package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.ArrayList;
import java.util.Collection;

public class GetExperimentGroupsResponse extends ArrayList<GetExperimentGroupsResponseData> {
    public GetExperimentGroupsResponse() {
        super();
    }

    public GetExperimentGroupsResponse(Collection<? extends GetExperimentGroupsResponseData> c) {
        super(c);
    }
}
