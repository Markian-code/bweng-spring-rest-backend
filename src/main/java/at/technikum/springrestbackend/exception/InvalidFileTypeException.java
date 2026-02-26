package at.technikum.springrestbackend.exception;

public class InvalidFileTypeException extends RuntimeException {

    public InvalidFileTypeException(final String message) {
        super(message);
    }
}