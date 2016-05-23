package cz.muni.sci.astro.fhm.core;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Contains helper functions
 *
 * @author Jan Hlava, 395986
 */
public class Helpers
{
    /**
     * Formats double - show max 9 fraction digits
     *
     * @param d double to format
     * @return string representation of double
     */
    public static String formatDouble(double d)
    {
        NumberFormat format = DecimalFormat.getInstance(Locale.US);
        format.setMaximumFractionDigits(9);
        format.setGroupingUsed(false);
        return format.format(d);
    }
}
