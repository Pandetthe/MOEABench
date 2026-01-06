package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.ArrayList;
import java.util.Collection;

public class CreateExperimentFullRequest extends ArrayList<CreateExperimentFullRequestData> {

    public CreateExperimentFullRequest() {
        super();
    }

    public CreateExperimentFullRequest(Collection<? extends CreateExperimentFullRequestData> c) {
        super(c);
    }
}
