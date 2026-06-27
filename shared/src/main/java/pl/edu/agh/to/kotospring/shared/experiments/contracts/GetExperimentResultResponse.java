package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.Collection;

@JsonDeserialize(contentAs = GetExperimentResultResponseData.class)
public class GetExperimentResultResponse extends ArrayList<GetExperimentResultResponseData> {
    public GetExperimentResultResponse() {
        super();
    }

    public GetExperimentResultResponse(Collection<? extends GetExperimentResultResponseData> c) {
        super(c);
    }
}
