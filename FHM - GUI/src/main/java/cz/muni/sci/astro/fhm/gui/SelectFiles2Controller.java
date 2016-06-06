package cz.muni.sci.astro.fhm.gui;

import cz.muni.sci.astro.fits.FitsException;
import cz.muni.sci.astro.fits.FitsFile;
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

    /**
     * Prepares this form - set files to listView, set multiple selection, listener for disabling Continue button and select all files, set reference to MainViewController (important for setting content in MainView)
     *
     * @param dir                directory, where files are located
     * @param files              file list to working with
     * @param mainViewController MainViewController controller
     */
    public void prepareWindow(String dir, List<String> files, MainViewController mainViewController) {
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
        List<String> errors = new ArrayList<>();
        for (String filename : files) {
            try (FitsFile ignored = new FitsFile(new File(dir + '/' + filename))) {
                listViewSelectedFiles.getItems().add(dir + '/' + filename);
            } catch (FitsException exc) {
                errors.add("Error: cannot load file " + filename + " with reason " + exc.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            GUIHelpers.showAlert(Alert.AlertType.ERROR, "Error", "Error occurred", String.join("\n", errors));
        }
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
        MultipleEditController multipleEditController = (MultipleEditController) mainViewController.setContent("fxml/MultipleEdit.fxml");
        multipleEditController.prepareWindow(listViewSelectedFiles.getSelectionModel().getSelectedItems(), mainViewController);
    }

    /**
     * Applies advanced filter and select results after filtering (filtering is using JavaScript eval)
     *
     * @return filtered FITS files
     */
    private List<String> applyFilter() {
        if (textAreaFiltering.getText().trim().isEmpty()) {
            return listViewSelectedFiles.getSelectionModel().getSelectedItems();
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
        for (String name : listViewSelectedFiles.getSelectionModel().getSelectedItems()) {
            try (FitsFile file = new FitsFile(new File(name))) {
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
            } catch (ScriptException | FitsException ignored) {
            } // if throws exception, then I consider it as not accepted file by conditions
        }
        if (result.isEmpty()) {
            buttonContinue.setDisable(true);
        }
        return result;
    }
}
