package spaceinvaders.services.exceptions;

public class InvalidAudioException extends Exception {

    public InvalidAudioException(String message) {
        super(message);
    }

    public InvalidAudioException(String message, Throwable cause) {
        super(message, cause);
    }
}
