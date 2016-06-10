package cz.muni.sci.astro.fhm.gui;

import cz.muni.sci.astro.fits.FitsCard;
import cz.muni.sci.astro.fits.FitsException;
import cz.muni.sci.astro.fits.FitsFile;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for second form of selecting files
 *
 * @author Jan Hlava, 395986
 */
public class SelectFiles2Controller {
    @FXML
    private MainViewController mainViewController;
    @FXML
    private ListView<String> listViewSelectedFiles;
    @FXML
    private TextArea textAreaFiltering;
    @FXML
    private Button buttonContinue;

    /**
     * Prepares this form - set files to listView, set multiple selection, listener for disabling Continue button and select all files, set reference to MainViewController (important for setting content in MainView)
     *
     * @param files              list of files (with path) to working with
     * @param mainViewController MainViewController controller
     */
    public void prepareWindow(List<String> files, MainViewController mainViewController) {
        this.mainViewController = mainViewController;
        listViewSelectedFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listViewSelectedFiles.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(new File(item).getName());
                }
            }
        });
        listViewSelectedFiles.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            buttonContinue.setDisable(listViewSelectedFiles.getSelectionModel().getSelectedItems().isEmpty());
        });
        listViewSelectedFiles.setItems(FXCollections.observableList(files));
        listViewSelectedFiles.getSelectionModel().selectAll();
    }

    /**
     * Handles click on button Preview - deselect all files and apply filter
     */
    @FXML
    private void handleClickButtonPreview() {
        listViewSelectedFiles.getSelectionModel().select(-1);
        applyFilter();
    }

    /**
     * Handles click on button Select all - select all files and request focus
     */
    @FXML
    private void handleClickButtonSelectAll() {
        listViewSelectedFiles.getSelectionModel().selectAll();
        listViewSelectedFiles.requestFocus();
    }

    /**
     * Handles click on button Continue - prepare next form
     */
    @FXML
    private void handleClickButtonContinue() {
        MultipleEditController multipleEditController = (MultipleEditController) mainViewController.setContent("fxml/MultipleEdit.fxml");
        multipleEditController.prepareWindow(listViewSelectedFiles.getSelectionModel().getSelectedItems(), mainViewController);
    }

    /**
     * Applies advanced filter and select results after filtering (filtering is using JavaScript eval)
     */
    private void applyFilter() {
        if (textAreaFiltering.getText().trim().isEmpty()) {
            listViewSelectedFiles.requestFocus();
        }
        // Convert syntax to JavaScript syntax
        String conditions = textAreaFiltering.getText().replace("&", "&&");
        conditions = conditions.replace("|", "||");
        conditions = conditions.replace("=", "==");
        conditions = conditions.replace("!==", "!=");
        conditions = conditions.replace("\"", "\\\"");
        conditions = conditions.replaceAll("(\\w*)[ ]?([!=]=)[ ]?([\\w ]*)", "$1 $2 \"$3\"");
        conditions = conditions.replaceAll("(\\w*)-(\\w*)[ ]?([!=]=)[ ]?([\\w ]*)", "$1__$2 $3 \"$4\"");
        final String finalConditions = conditions;
        Task<List<String>> task = new Task<List<String>>() {
            @Override
            protected List<String> call() throws Exception {
                int countFiles = listViewSelectedFiles.getItems().size();
                List<String> result = new ArrayList<>(countFiles);
                ScriptEngine engine;
                updateProgress(0, countFiles);
                int counter = 0;
                for (String name : listViewSelectedFiles.getItems()) {
                    if (isCancelled()) {
                        break;
                    }
                    File fFile = new File(name);
                    updateTitle(fFile.getName());
                    try (FitsFile file = new FitsFile(fFile)) {
                        boolean accepted = true;
                        for (int i = 0; i < file.getCountHDUs(); i++) {
                            engine = new ScriptEngineManager().getEngineByName("JavaScript");
                            engine.eval(file.getHDU(i).getHeader().getJavaScriptKeywordsAndValues());
                            // Evaluate (card is definition, filtering is formula to test)
                            if (!engine.eval(finalConditions).equals(true)) {
                                accepted = false;
                            }
                        }
                        if (accepted) {
                            result.add(name);
                        }
                    } catch (ScriptException | FitsException ignored) { // if throws exception, then I consider it as not accepted file by conditions
                        if (isCancelled()) {
                            break;
                        }
                    } finally {
                        counter++;
                        updateProgress(counter, countFiles);
                    }
                }
                return result;
            }
        };
        Stage progressInfo = GUIHelpers.createProgressInfo("Filtering fits files. Please wait.", task);
        task.setOnSucceeded(event -> {
            progressInfo.close();
            if (task.getValue().isEmpty()) {
                buttonContinue.setDisable(true);
            }
            for (String name : task.getValue()) {
                listViewSelectedFiles.getSelectionModel().select(name);
            }
            listViewSelectedFiles.requestFocus();
        });
        EventHandler<WorkerStateEvent> failAction = event -> {
            progressInfo.close();
            buttonContinue.setDisable(true);
            listViewSelectedFiles.requestFocus();
            GUIHelpers.showAlert(Alert.AlertType.ERROR, "Error with filtering fits files", "Error occurs while filtering fits files. Try it again please.", "");
        };
        task.setOnCancelled(failAction);
        task.setOnFailed(failAction);
        progressInfo.show();
        Thread thread = new Thread(task);
        thread.start();
    }
}
