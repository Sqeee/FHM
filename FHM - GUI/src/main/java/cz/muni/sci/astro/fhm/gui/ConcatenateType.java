package cz.muni.sci.astro.fhm.gui;

import cz.muni.sci.astro.fhm.core.OperationConcatenate;

/**
 * Enum represents concatenation types
 *
 * @author Jan Hlava, 395986
 */
public enum ConcatenateType {
    STRING,
    KEYWORD_VALUE;

    /**
     * Returns prefix for this type
     *
     * @return prefix for this type
     */
    public String getPrefix() {
        if (this == STRING) {
            return OperationConcatenate.PREFIX_STRING;
        } else if (this == KEYWORD_VALUE) {
            return OperationConcatenate.PREFIX_KEYWORD_VALUE;
        } else {
            return "Unknown";
        }
    }

    /**
     * Returns string representation
     *
     * @return string representation
     */
    public String toString() {
        if (this == STRING) {
            return "String";
        } else if (this == KEYWORD_VALUE) {
            return "Keyword value";
        } else {
            return "Unknown";
        }
    }
}
