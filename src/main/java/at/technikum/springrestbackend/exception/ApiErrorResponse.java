package at.technikum.springrestbackend.exception;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ApiErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<String> details = new ArrayList<>();

    public ApiErrorResponse() {
    }

    public ApiErrorResponse(
            final LocalDateTime timestamp,
            final int status,
            final String error,
            final String message,
            final String path
    ) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public ApiErrorResponse(
            final LocalDateTime timestamp,
            final int status,
            final String error,
            final String message,
            final String path,
            final List<String> details
    ) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setTimestamp(final LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setStatus(final int status) {
        this.status = status;
    }

    public void setError(final String error) {
        this.error = error;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public void setDetails(final List<String> details) {
        this.details = details;
    }
}