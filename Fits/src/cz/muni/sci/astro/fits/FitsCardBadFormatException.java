package cz.muni.sci.astro.fits;

/**
 * Represents exception for bad format of FITS card
 *
 * @author Jan Hlava, 395986
 */
public class FitsCardBadFormatException extends FitsException {
    /**
     * Creates exception with given message
     *
     * @param message description of exception
     */
    public FitsCardBadFormatException(String message) {
        super(message);
    }

    /**
     * Creates exception with given message and cause
     *
     * @param message description of exception
     * @param cause   cause of exception
     */
    public FitsCardBadFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
