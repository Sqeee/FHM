package cz.muni.sci.astro.fhm.gui;

/**
 * Class represents concatenation value
 *
 * @author Jan Hlava, 395986
 */
public class ConcatenateValue
{
    private ConcatenateType type;
    private String value;

    /**
     * Creates instance of this class
     *
     * @param type type of concatenation
     * @param value value of concatenation
     */
    public ConcatenateValue(ConcatenateType type, String value)
    {
        this.type = type;
        this.value = value;
    }

    /**
     * Returns string representation of concatenation value
     *
     * @return string representation of concatenation value
     */
    @Override
    public String toString()
    {
        return type.toString() + ": \"" + value + "\"";
    }

    /**
     * Returns export representation of concatenation value, it is good for processing
     *
     * @return export representation of concatenation value, it is good for processing
     */
    public String toExport()
    {
        return type.getPrefix() + value;
    }
}
