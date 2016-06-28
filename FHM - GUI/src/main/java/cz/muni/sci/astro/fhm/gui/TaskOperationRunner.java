package cz.muni.sci.astro.fhm.gui;

import javafx.concurrent.Task;

/**
 * Abstract class for special needs of multiple operations runner
 *
 * @param <T> type of task
 * @author Jan Hlava, 395986
 */
public abstract class TaskOperationRunner<T> extends Task<T> {
    /**
     * Updates progress of task
     *
     * @param workDone how many things is done
     * @param max      max things to be done
     */
    @Override
    public void updateProgress(double workDone, double max) {
        super.updateProgress(workDone, max);
    }

    /**
     * Updates title of task
     *
     * @param title name of progressed thing
     */
    @Override
    public void updateTitle(String title) {
        super.updateTitle(title);
    }

    /**
     * Returns true if task is cancelled
     *
     * @return true if task is cancelled
     */
    @Override
    public boolean isCancelled() {
        return super.isCancelled();
    }
}
