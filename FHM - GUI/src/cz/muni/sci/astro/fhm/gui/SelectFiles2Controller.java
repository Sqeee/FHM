package cz.muni.sci.astro.fhm.gui;

import cz.muni.sci.astro.fits.FitsException;
import cz.muni.sci.astro.fits.FitsFile;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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

    private List<String> files;
    private String dir;

    /**
     * Stores selected file list from previous form
     *
     * @param files file list to working with
     */
    public void setFiles(List<String> files) {
        this.files = files;
    }

    /**
     * Stores path to directory, where files are located
     *
     * @param dir directory, where files are located
     */
    public void setDir(String dir) {
        this.dir = dir;
    }

    /**
     * Sets reference to MainViewController (important for setting content in MainView)
     *
     * @param mainViewController MainViewController controller
     */
    public void setMainViewController(MainViewController mainViewController) {
        this.mainViewController = mainViewController;
    }

    /**
     * Prepares this form - set files to listView, set multiple selection, listener for disabling Continue button and select all files
     */
    public void prepareWindow() {
        listViewSelectedFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listViewSelectedFiles.setItems(FXCollections.observableList(files));
        listViewSelectedFiles.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            buttonContinue.setDisable(listViewSelectedFiles.getSelectionModel().getSelectedItems().isEmpty());
        });
        listViewSelectedFiles.getSelectionModel().selectAll();
    }

    /**
     * Handles click on button Preview - deselect all files, apply filter and request focus
     */
    @FXML
    private void handleClickButtonPreview() {
        listViewSelectedFiles.getSelectionModel().select(-1);
        for (String name : applyFilter()) {
            listViewSelectedFiles.getSelectionModel().select(name);
        }
        listViewSelectedFiles.requestFocus();
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
        MultipleEditController multipleEditController = (MultipleEditController) mainViewController.setContent("MultipleEdit.fxml");
        multipleEditController.setMainViewController(mainViewController);
        multipleEditController.setDir(dir);
        multipleEditController.setFiles(listViewSelectedFiles.getSelectionModel().getSelectedItems());
        multipleEditController.prepareWindow();
    }

    /**
     * Applies advanced filter and select results after filtering (filtering is using JavaScript eval)
     *
     * @return filtered FITS files
     */
    private List<String> applyFilter() {
        if (textAreaFiltering.getText().trim().isEmpty()) {
            return files;
        }
        List<String> result = new ArrayList<>();
        // Convert syntax to JavaScript syntax
        String conditions = textAreaFiltering.getText().replace("&", "&&");
        conditions = conditions.replace("|", "||");
        conditions = conditions.replace("=", "==");
        conditions = conditions.replace("!==", "!=");
        conditions = conditions.replace("\"", "\\\"");
        conditions = conditions.replaceAll("(\\w*)[ ]?([!=]=)[ ]?([\\w ]*)", "$1 $2 \"$3\"");
        conditions = conditions.replaceAll("(\\w*)-(\\w*)[ ]?([!=]=)[ ]?([\\w ]*)", "$1__$2 $3 \"$4\"");

        ScriptEngine engine;
        FitsFile file;
        for (String name : files) {
            try {
                file = new FitsFile(new File(dir + '/' + name));
            } catch (FitsException exc) {
                continue;
            }
            try {
                boolean accepted = true;
                for (int i = 0; i < file.getCountHDUs(); i++) {
                    engine = new ScriptEngineManager().getEngineByName("JavaScript");
                    engine.eval(file.getHDU(i).getHeader().getJavaScriptKeywordsAndValues());
                    // Evaluate (card is definition, filtering is formula to test)
                    if (!engine.eval(conditions).equals(true)) {
                        accepted = false;
                    }
                }
                if (accepted) {
                    result.add(name);
                }
            } catch (ScriptException ignored) // if throws exception, then I consider it as not accepted file by conditions
            {
            } finally {
                file.closeFile();
            }
        }
        if (result.isEmpty()) {
            buttonContinue.setDisable(true);
        }
        return result;
    }
}
