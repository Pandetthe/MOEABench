package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.Collection;

@JsonDeserialize(contentAs = GetExperimentAggregateData.class)
public class GetExperimentAggregateResponse extends ArrayList<GetExperimentAggregateData> {
    public GetExperimentAggregateResponse() {
        super();
    }

    public GetExperimentAggregateResponse(Collection<? extends GetExperimentAggregateData> c) {
        super(c);
    }
}
