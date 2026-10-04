package io.battery.model;

/**
 * Thrown when the model is invalid in a way that bean validation doesn't cover, such as
 * duplicate step names or a step path that doesn't resolve to a file.
 */
public class ModelException extends RuntimeException {
    public ModelException(String message) {
        super(message);
    }
}
