package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.ArrayList;
import java.util.Collection;


public class GetExperimentsResponse extends ArrayList<GetExperimentsResponseData> {
    public GetExperimentsResponse() {
        super();
    }

    public GetExperimentsResponse(Collection<? extends GetExperimentsResponseData> c) {
        super(c);
    }
}
