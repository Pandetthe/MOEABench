package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.Collection;

@JsonDeserialize(contentAs = GetExperimentGroupsResponseData.class)
public class GetExperimentGroupsResponse extends ArrayList<GetExperimentGroupsResponseData> {
    public GetExperimentGroupsResponse() {
        super();
    }

    public GetExperimentGroupsResponse(Collection<? extends GetExperimentGroupsResponseData> c) {
        super(c);
    }
}
