package spaceinvaders.services.exceptions;

// Thrown when an image resource fails to load
public class InvalidImageException extends Exception {
    public InvalidImageException(String message) {
        super(message);
    }

    public InvalidImageException(String message, Throwable cause) {
        super(message, cause);
    }
}