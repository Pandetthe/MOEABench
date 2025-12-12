package pl.edu.agh.to.kotospring.server.extensions;

import org.moeaframework.algorithm.extension.Frequency;
import org.moeaframework.algorithm.extension.PeriodicExtension;
import org.moeaframework.core.population.NondominatedPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edu.agh.to.kotospring.server.entities.ExperimentPart;
import org.moeaframework.algorithm.Algorithm;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.to.kotospring.server.repositories.ExperimentPartRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Optional;

public class DbCheckpointExtension extends PeriodicExtension {
    private final Logger logger = LoggerFactory.getLogger(DbCheckpointExtension.class);

    private final ExperimentPartRepository repository;
    private final Long experimentPartId;

    public DbCheckpointExtension(ExperimentPartRepository repository, Long experimentPartId, Frequency frequency) {
        super(frequency);
        this.repository = repository;
        this.experimentPartId = experimentPartId;
        NondominatedPopulation a;
    }

    @Transactional
    protected void saveToDbTransactional(Algorithm algorithm) throws IOException {
        byte[] stateBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(baos))) {
            algorithm.saveState(oos);
            oos.flush();
            stateBytes = baos.toByteArray();
        }

        Optional<ExperimentPart> optional = repository.findById(experimentPartId);
        if (optional.isEmpty()) {
            logger.warn("ExperimentPart with id {} not found. Checkpoint not saved.", experimentPartId);
            return;
        }

        ExperimentPart part = optional.get();
        part.setState(stateBytes);
        repository.save(part);
    }

    private void saveToDb(Algorithm algorithm) throws IOException {
        saveToDbTransactional(algorithm);
    }

    @Transactional(readOnly = true)
    protected void loadFromDbTransactional(Algorithm algorithm) throws IOException, ClassNotFoundException {
        Optional<ExperimentPart> optional = repository.findById(experimentPartId);
        if (optional.isEmpty()) {
            return;
        }
        byte[] stateBytes = optional.get().getState();
        if (stateBytes == null || stateBytes.length == 0) {
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(stateBytes)))) {
            algorithm.loadState(ois);
        }
    }

    private void loadFromDb(Algorithm algorithm) throws IOException, ClassNotFoundException {
        loadFromDbTransactional(algorithm);
    }

    @Override
    public void doAction(Algorithm algorithm) {
        try {
            this.saveToDb(algorithm);
        } catch (IOException e) {
            logger.warn("Unable to write checkpoint to DB, continuing without checkpoints!", e);
        }
    }

    @Override
    public void onRegister(Algorithm algorithm) {
        super.onRegister(algorithm);
        try {
            this.loadFromDb(algorithm);
        } catch (ClassNotFoundException | IOException e) {
            logger.warn("Unable to read checkpoint from DB, continuing without checkpoints!", e);
        }
    }

    @Override
    public void onTerminate(Algorithm algorithm) {
        this.doAction(algorithm);
        super.onTerminate(algorithm);
    }
}