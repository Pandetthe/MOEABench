package pl.edu.agh.to.kotospring.shared.experiments.contracts;

import java.util.ArrayList;
import java.util.Collection;

public class CreateExperimentRequest extends ArrayList<CreateExperimentRequestData> {
    public CreateExperimentRequest() {
        super();
    }

    public CreateExperimentRequest(Collection<? extends CreateExperimentRequestData> c) {
        super(c);
    }
}