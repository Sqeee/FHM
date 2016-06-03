package cz.muni.sci.astro.fits;

/**
 * Represents exception for errors in FITS files
 *
 * @author Jan Hlava, 395986
 */
public class FitsFileException extends FitsException {
    /**
     * Creates exception with given message
     *
     * @param message description of exception
     */
    public FitsFileException(String message) {
        super(message);
    }

    /**
     * Creates exception with given message and cause
     *
     * @param message description of exception
     * @param cause   cause of exception
     */
    public FitsFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
