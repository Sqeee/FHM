package cz.muni.sci.astro.fhm.gui;

import cz.muni.sci.astro.fhm.core.MultipleOperationsRunner;

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
            return MultipleOperationsRunner.PARAM_CONCATENATION_STRING_PREFIX;
        } else if (this == KEYWORD_VALUE) {
            return MultipleOperationsRunner.PARAM_CONCATENATION_KEYWORD_VALUE_PREFIX;
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
