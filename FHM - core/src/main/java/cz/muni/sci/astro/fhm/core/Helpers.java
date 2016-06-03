package cz.muni.sci.astro.fhm.core;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Contains helper functions
 *
 * @author Jan Hlava, 395986
 */
public class Helpers {
    /**
     * Formats double - show max 9 fraction digits
     *
     * @param number double number to format
     * @return string representation of double
     */
    public static String formatDouble(double number) {
        NumberFormat format = DecimalFormat.getInstance(Locale.US);
        format.setMaximumFractionDigits(9);
        format.setGroupingUsed(false);
        return format.format(number);
    }
}
