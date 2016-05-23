package cz.muni.sci.astro.fits;

/**
 * Represents exception for unknown format of FITS card date value
 *
 * @author Jan Hlava, 395986
 */
public class FitsCardDateValueUnknownFormatException extends FitsException
{
    /**
     * Creates exception with given message
     *
     * @param message description of exception
     */
    public FitsCardDateValueUnknownFormatException(String message)
    {
        super(message);
    }

    /**
     * Creates exception with given message and cause
     *
     * @param message description of exception
     * @param cause cause of exception
     */
    public FitsCardDateValueUnknownFormatException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
