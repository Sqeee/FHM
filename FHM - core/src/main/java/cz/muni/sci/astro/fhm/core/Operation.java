package cz.muni.sci.astro.fhm.core;

import cz.muni.sci.astro.fits.FitsFile;

/**
 * Interface for operation type
 *
 * @author Jan Hlava, 395986
 */
public interface Operation {
    /**
     * Executes given operation
     *
     * @param file         file for executing operation
     * @param printerOK    method for printing OK things
     * @param printerError method for printing error things
     */
    void execute(FitsFile file, PrintOutputMethod printerOK, PrintOutputMethod printerError);
}
