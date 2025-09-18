package bessa.morangon.rafael.TaskFlow.domain.configuration.exceptions;

public class UnauthorizedAccessException extends RuntimeException{
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
