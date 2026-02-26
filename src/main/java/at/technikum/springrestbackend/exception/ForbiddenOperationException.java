package at.technikum.springrestbackend.exception;

public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(final String message) {
        super(message);
    }
}