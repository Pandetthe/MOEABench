package pl.edu.agh.to.kotospring.server.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NotAllPartsFinishedException extends RuntimeException {
    public NotAllPartsFinishedException() {
        super("Not all parts have finished.");
    }
}