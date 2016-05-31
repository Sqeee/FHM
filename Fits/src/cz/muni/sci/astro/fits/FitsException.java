package cz.muni.sci.astro.fits;

/**
 * Represents general exception for FITS files
 *
 * @author Jan Hlava, 395986
 */
public class FitsException extends Exception {
    /**
     * Creates exception with given message
     *
     * @param message description of exception
     */
    public FitsException(String message) {
        super(message);
    }

    /**
     * Creates exception with given message and cause
     *
     * @param message description of exception
     * @param cause   cause of exception
     */
    public FitsException(String message, Throwable cause) {
        super(message, cause);
    }
}
