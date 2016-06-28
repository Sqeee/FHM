package cz.muni.sci.astro.fhm.gui;

import cz.muni.sci.astro.fits.FitsException;
import cz.muni.sci.astro.fits.FitsFile;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Controller for first form of selecting files
 */
public class SelectFiles1Controller {
    private static final String INVALID_DIR_TEXTFIELD_STYLE = "-fx-background-color: lightcoral;";
    private static final String PREFERENCE_DEFAULT_DIR = "Default_dir";
    private static final String DEFAULT_FILTER = "*";
    private static final Label LABEL_NO_MATCHING = new Label("No FITS files matching filename filter in this directory");
    private static final Label LABEL_NO_FITS = new Label("No FITS files in this directory");
    private static final Label LABEL_NO_VALID_DIR = new Label("No valid directory");
    @FXML
    private Button buttonChooseDir;
    @FXML
    private TextField textFieldChooseDir;
    @FXML
    private ListView<String> listViewFiles;
    @FXML
    private Label labelFiltering;
    @FXML
    private Label labelFilteringFilename;
    @FXML
    private TextField textFieldFilteringFilename;
    @FXML
    private Button buttonApplyFilter;
    @FXML
    private Button buttonContinue;
    @FXML
    private Button buttonSelectAll;
    @FXML
    private Button buttonDeselectAll;
    private Preferences prefs;
    private String filter = DEFAULT_FILTER;
    private MainViewController mainViewController;

    /**
     * Handles click on button Choose dir - prepare dir choose dialog, get result from dialog
     */
    @FXML
    protected void handleClickButtonChooseDir() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        if (!textFieldChooseDir.getText().isEmpty() && !textFieldChooseDir.getStyle().equals(INVALID_DIR_TEXTFIELD_STYLE)) {
            dirChooser.setInitialDirectory(new File(textFieldChooseDir.getText()));
        }
        dirChooser.setTitle("Choose directory containing FITS files");
        File default_dir = getDefaultDir();
        if (default_dir != null) {
            dirChooser.setInitialDirectory(default_dir);
        }
        File dir = dirChooser.showDialog(buttonChooseDir.getScene().getWindow());
        if (dir != null) {
            textFieldChooseDir.setText(dir.getAbsolutePath());
        }
    }

    /**
     * Handles click on button Apply filter - apply filter
     */
    @FXML
    protected void handleApplyFilter() {
        listViewFiles.getSelectionModel().clearSelection();
        filter = DEFAULT_FILTER;
        if (!textFieldFilteringFilename.getText().isEmpty()) {
            filter = textFieldFilteringFilename.getText();
        }
        updateListViewFiles(textFieldChooseDir.getText());
    }

    /**
     * Handles click on button Continue - save directory to preferences, prepare next form
     */
    @FXML
    protected void handleClickButtonContinue() {
        final String dir = textFieldChooseDir.getText();
        prefs.put(PREFERENCE_DEFAULT_DIR, dir);
        Task<Pair<List<String>, List<String>>> task = new Task<Pair<List<String>, List<String>>>() {
            @Override
            protected Pair<List<String>, List<String>> call() throws Exception {
                int countFiles = listViewFiles.getSelectionModel().getSelectedItems().size();
                List<String> files = new ArrayList<>(countFiles);
                List<String> errors = new ArrayList<>();
                updateProgress(0, countFiles);
                int counter = 0;
                for (String filename : listViewFiles.getSelectionModel().getSelectedItems()) {
                    if (isCancelled()) {
                        break;
                    }
                    updateTitle(filename);
                    String path = dir + '/' + filename;
                    try (FitsFile ignored = new FitsFile(new File(path))) {
                        files.add(path);
                    } catch (FitsException exc) {
                        if (isCancelled()) {
                            break;
                        }
                        errors.add("Error: cannot load file " + filename + " with reason " + exc.getMessage());
                    }
                    counter++;
                    updateProgress(counter, countFiles);
                }
                return new Pair<>(files, errors);
            }
        };
        Stage progressInfo = GUIHelpers.createProgressInfo("Loading fits files from directory. Please wait.", task);
        task.setOnSucceeded(event -> {
            progressInfo.close();
            List<String> files = task.getValue().getKey();
            List<String> errors = task.getValue().getValue();
            if (!errors.isEmpty()) {
                if (files.isEmpty()) {
                    GUIHelpers.showAlert(Alert.AlertType.ERROR, "Error with loading files", "None of selected files can be loaded.", String.join("\n", errors));
                } else {
                    GUIHelpers.showAlert(Alert.AlertType.CONFIRMATION, "Error with loading files", "Some selected files cannot be loaded. Do you want to continue without them?", String.join("\n", errors)).ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            loadNextForm(files);
                        }
                    });
                }
            } else {
                loadNextForm(files);
            }
        });
        EventHandler<WorkerStateEvent> failAction = event -> {
            progressInfo.close();
            GUIHelpers.showAlert(Alert.AlertType.ERROR, "Error with loading fits files", "Error occurs while loading fits files. Try it again please.", "");
        };
        task.setOnFailed(failAction);
        task.setOnCancelled(failAction);
        progressInfo.show();
        Thread thread = new Thread(task);
        thread.start();
    }

    /**
     * Handles click on button Select all - select all files
     */
    @FXML
    protected void handleClickButtonSelectAll() {
        listViewFiles.getSelectionModel().selectAll();
        listViewFiles.requestFocus();
    }

    /**
     * Handles click on button Deselect all - deselect all files
     */
    @FXML
    protected void handleClickButtonDeselectAll() {
        listViewFiles.getSelectionModel().clearSelection();
    }

    /**
     * Returns default dir for FITS files stored in user preferences
     *
     * @return default dir for FITS files stored in user preferences, if dir does not exist returns null
     */
    private File getDefaultDir() {
        String default_dir_pathname = prefs.get(PREFERENCE_DEFAULT_DIR, "");
        if (default_dir_pathname.isEmpty()) {
            return null;
        }
        File default_dir = new File(default_dir_pathname);
        if (default_dir.exists() && default_dir.isDirectory()) {
            return default_dir;
        } else {
            return null;
        }
    }

    /**
     * Updates ListView with FITS files in given dirname
     *
     * @param dirname directory for which content should be ListView showed
     */
    private void updateListViewFiles(String dirname) {
        if (!dirname.isEmpty()) {
            Path dir;
            try {
                dir = Paths.get(dirname.trim());
            } catch (InvalidPathException exc) {
                dir = null;
            }
            if (dir != null && Files.isDirectory(dir)) { // Dir is valid directory
                activateFilteringOptions(true);
                textFieldChooseDir.setStyle("");
                listFiles(dir);
            } else {
                activateFilteringOptions(false);
                listViewFiles.setItems(null);
                listViewFiles.setPlaceholder(LABEL_NO_VALID_DIR);
                textFieldChooseDir.setStyle(INVALID_DIR_TEXTFIELD_STYLE);
            }
        } else {
            activateFilteringOptions(false);
            listViewFiles.setItems(null);
            listViewFiles.setPlaceholder(LABEL_NO_VALID_DIR);
            textFieldChooseDir.setStyle("");
        }
    }

    /**
     * Sets files to list view
     *
     * @param files files, which should be viewed in list view
     */
    private void setListViewFiles(List<String> files) {
        if (files != null && !files.isEmpty()) { // Some files left after applying filter
            listViewFiles.setItems(FXCollections.observableList(files));
            activateFilteringOptions(true);
        } else {
            listViewFiles.setItems(null);
            if (!filter.equals(DEFAULT_FILTER)) { // Used filtering
                activateFilteringOptions(true);
                listViewFiles.setPlaceholder(LABEL_NO_MATCHING);
            } else {
                activateFilteringOptions(false);
                listViewFiles.setPlaceholder(LABEL_NO_FITS);
            }
        }
    }

    /**
     * Enables/Disables filtering option
     *
     * @param activate true for activating filtering option, false for deactivating
     */
    private void activateFilteringOptions(boolean activate) {
        labelFiltering.setDisable(!activate);
        labelFilteringFilename.setDisable(!activate);
        textFieldFilteringFilename.setDisable(!activate);
        buttonApplyFilter.setDisable(!activate);
        buttonDeselectAll.setDisable(!activate);
        buttonSelectAll.setDisable(!activate);
    }

    /**
     * Filters FITS files from giving directory
     *
     * @param dir directory containing FITS files
     */
    private void listFiles(Path dir) {
        Task<List<String>> task = new Task<List<String>>() {
            @Override
            protected List<String> call() throws Exception {
                updateTitle(dir.toString());
                List<String> files = new ArrayList<>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {
                    for (Path file : stream) {
                        if (isCancelled()) {
                            break;
                        }
                        if (Files.isRegularFile(file) && FitsFile.isFitsFile(file.toFile())) {
                            files.add(file.getFileName().toString());
                        }
                    }
                    return files;
                } catch (IOException exc) {
                    return null;
                }
            }
        };
        Stage progressInfo = GUIHelpers.createProgressInfo("Reading files from directory. Please wait.", task);
        task.setOnSucceeded(event -> {
            progressInfo.close();
            setListViewFiles(task.getValue());
        });
        EventHandler<WorkerStateEvent> failAction = event -> {
            progressInfo.close();
            setListViewFiles(null);
        };
        task.setOnFailed(failAction);
        task.setOnCancelled(failAction);
        progressInfo.show();
        Thread thread = new Thread(task);
        thread.start();
    }

    /**
     * Loads next form - SelectFiles2
     *
     * @param files list of selected valid fits files with paths
     */
    private void loadNextForm(List<String> files) {
        SelectFiles2Controller selectFiles2Controller = (SelectFiles2Controller) mainViewController.setContent("fxml/SelectFiles2.fxml");
        selectFiles2Controller.prepareWindow(files, mainViewController);
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
     * Initializes this controller - add listeners, set multiple selection mode and get user preferences
     */
    @FXML
    public void initialize() {
        prefs = Preferences.userNodeForPackage(SelectFiles1Controller.class);
        listViewFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listViewFiles.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
        {
            buttonContinue.setDisable(newValue == null);
        });
        listViewFiles.setPlaceholder(LABEL_NO_VALID_DIR);
        textFieldChooseDir.textProperty().addListener((observable, oldValue, newValue) ->
        {
            updateListViewFiles(newValue);
        });
    }
}
