package cz.muni.sci.astro.fits;

/**
 * Enum definitions of known image types
 *
 * @author Jan Hlava, 395986
 */
public enum FitsImageType
{
    UNKNOWN("Unknown"),
    LIGHT_FRAME("Light Frame"),
    FLAT_FIELD("Flat Field"),
    DARK_FRAME("Dark Frame"),
    BIAS_FRAME("Bias Frame"),
    OBJECT("Object");

    private final String type;

    /**
     * Creates new instance of this enum with given type
     *
     * @param type type of image type
     */
    FitsImageType(String type)
    {
        this.type = type;
    }

    /**
     * Returns string representation of enum
     *
     * @return string representation of enum
     */
    @Override
    public String toString()
    {
        return type;
    }
}
