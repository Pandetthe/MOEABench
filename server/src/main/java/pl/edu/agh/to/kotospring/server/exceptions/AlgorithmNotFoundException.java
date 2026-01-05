package pl.edu.agh.to.kotospring.server.exceptions;

public class AlgorithmNotFoundException extends NotFoundException {
    public AlgorithmNotFoundException(String algorithm) {
        super("Algorithm not found: " + algorithm);
    }
}
