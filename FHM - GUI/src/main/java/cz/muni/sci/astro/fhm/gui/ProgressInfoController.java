package cz.muni.sci.astro.fhm.gui;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

/**
 * Controller for progress info indicator
 *
 * @author Jan Hlava, 395986
 */
public class ProgressInfoController {
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label labelProcessInfo;
    @FXML
    private Label labelActualProcessing;

    /**
     * Prepares window with progress info
     *
     * @param processInfo information about process
     * @param task        task of process
     */
    public void prepareWindow(String processInfo, Task<?> task) {
        labelProcessInfo.setText(processInfo);
        labelActualProcessing.textProperty().bind(task.titleProperty());
        progressIndicator.progressProperty().bind(task.progressProperty());
    }
}
