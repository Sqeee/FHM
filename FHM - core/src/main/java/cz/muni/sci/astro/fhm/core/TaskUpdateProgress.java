package cz.muni.sci.astro.fhm.core;

/**
 * Interface for updating progress in tasks
 *
 * @author Jan Hlava, 395986
 */
public interface TaskUpdateProgress {
    void done(double workDone, double max);
}
