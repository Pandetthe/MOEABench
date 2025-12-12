package pl.edu.agh.to.kotospring.server.exceptions;

public class ProblemNotFoundException extends NotFoundException {
    public ProblemNotFoundException(String algorithm) {
        super("Algorithm not found: " + algorithm);
    }
}
