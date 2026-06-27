package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.Collection;

@JsonDeserialize(contentAs = GetExperimentRunResultResponseData.class)
public class GetExperimentRunResultResponse extends ArrayList<GetExperimentRunResultResponseData> {
    public GetExperimentRunResultResponse() {
        super();
    }

    public GetExperimentRunResultResponse(Collection<? extends GetExperimentRunResultResponseData> c) {
        super(c);
    }
}