package cz.muni.sci.astro.fhm.core;

/**
 * Represents exception for illegal argument in operation
 *
 * @author Jan Hlava, 395986
 */
public class OperationIllegalArgumentException extends Exception {
    /**
     * Creates exception with given message
     *
     * @param message description of exception
     */
    public OperationIllegalArgumentException(String message) {
        super(message);
    }

    /**
     * Creates exception with given message and cause
     *
     * @param message description of exception
     * @param cause   cause of exception
     */
    public OperationIllegalArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
