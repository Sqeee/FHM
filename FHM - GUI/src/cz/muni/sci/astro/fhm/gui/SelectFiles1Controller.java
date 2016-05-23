package cz.muni.sci.astro.fhm.gui;

import cz.muni.sci.astro.fits.FitsFile;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Controller for first form of selecting files
 */
public class SelectFiles1Controller
{
    @FXML private Button buttonChooseDir;
    @FXML private TextField textFieldChooseDir;
    @FXML private ListView<String> listViewFiles;
    @FXML private Label labelFiltering;
    @FXML private Label labelFilteringFilename;
    @FXML private TextField textFieldFilteringFilename;
    @FXML private Button buttonApplyFilter;
    @FXML private Button buttonContinue;
    @FXML private Button buttonSelectAll;
    @FXML private Button buttonDeselectAll;

    private static final String INVALID_DIR_TEXTFIELD_STYLE = "-fx-background-color: lightcoral;";
    private static final String PREFERENCE_DEFAULT_DIR = "Default_dir";
    private static final String DEFAULT_FILTER = "*";
    private static final Label LABEL_NO_MATCHING = new Label("No FITS files matching filename filter in this directory");
    private static final Label LABEL_NO_FITS = new Label("No FITS files in this directory");
    private static final Label LABEL_NO_VALID_DIR = new Label("No valid directory");
    private Preferences prefs;
    private String filter = DEFAULT_FILTER;
    private MainViewController mainViewController;

    /**
     * Handles click on button Choose dir - prepare dir choose dialog, get result from dialog
     */
    @FXML protected void handleClickButtonChooseDir()
    {
        DirectoryChooser dirChooser = new DirectoryChooser();
        if (!textFieldChooseDir.getText().isEmpty() && !textFieldChooseDir.getStyle().equals(INVALID_DIR_TEXTFIELD_STYLE))
        {
            dirChooser.setInitialDirectory(new File(textFieldChooseDir.getText()));
        }
        dirChooser.setTitle("Choose directory containing FITS files");
        File default_dir = getDefaultDir();
        if (default_dir != null)
        {
            dirChooser.setInitialDirectory(default_dir);
        }
        File dir = dirChooser.showDialog(buttonChooseDir.getScene().getWindow());
        if (dir != null)
        {
            textFieldChooseDir.setText(dir.getAbsolutePath());
        }
    }

    /**
     * Handles click on button Apply filter - apply filter
     */
    @FXML protected void handleApplyFilter()
    {
        listViewFiles.getSelectionModel().clearSelection();
        filter = DEFAULT_FILTER;
        if (!textFieldFilteringFilename.getText().isEmpty())
        {
            filter = textFieldFilteringFilename.getText();
        }
        updateListViewFiles(textFieldChooseDir.getText());
    }

    /**
     * Handles click on button Continue - save directory to preferences, prepare next form
     */
    @FXML protected void handleClickButtonContinue()
    {
        prefs.put(PREFERENCE_DEFAULT_DIR, textFieldChooseDir.getText());
        SelectFiles2Controller selectFiles2Controller = (SelectFiles2Controller) mainViewController.setContent("SelectFiles2.fxml");
        selectFiles2Controller.setMainViewController(mainViewController);
        selectFiles2Controller.setDir(textFieldChooseDir.getText());
        selectFiles2Controller.setFiles(listViewFiles.getSelectionModel().getSelectedItems());
        selectFiles2Controller.prepareWindow();
    }

    /**
     * Handles click on button Select all - select all files
     */
    @FXML protected void handleClickButtonSelectAll()
    {
        listViewFiles.getSelectionModel().selectAll();
        listViewFiles.requestFocus();
    }

    /**
     * Handles click on button Deselect all - deselect all files
     */
    @FXML protected void handleClickButtonDeselectAll()
    {
        listViewFiles.getSelectionModel().clearSelection();
    }

    /**
     * Returns default dir for FITS files stored in user preferences
     *
     * @return default dir for FITS files stored in user preferences, if dir does not exist returns null
     */
    private File getDefaultDir()
    {
        String default_dir_pathname = prefs.get(PREFERENCE_DEFAULT_DIR, "");
        if (default_dir_pathname.isEmpty())
        {
            return null;
        }
        File default_dir = new File(default_dir_pathname);
        if (default_dir.exists() && default_dir.isDirectory())
        {
            return default_dir;
        }
        else
        {
            return null;
        }
    }

    /**
     * Updates ListView with FITS files in given dirname
     *
     * @param dirname directory for which content should be ListView showed
     */
    private void updateListViewFiles(String dirname)
    {
        if (!dirname.isEmpty())
        {
            Path dir;
            try
            {
                dir = Paths.get(dirname.trim());
            }
            catch (InvalidPathException exc)
            {
                dir = null;
            }
            if (dir != null && Files.isDirectory(dir)) // Dir is valid directory
            {
                activateFilteringOptions(true);
                textFieldChooseDir.setStyle("");
                List<String> files = listFiles(dir);
                if (files != null && !files.isEmpty()) // Some files left after applying filter
                {
                    listViewFiles.setItems(FXCollections.observableList(files));
                    activateFilteringOptions(true);
                }
                else
                {
                    listViewFiles.setItems(null);
                    if (!filter.equals(DEFAULT_FILTER)) // Used filtering
                    {
                        activateFilteringOptions(true);
                        listViewFiles.setPlaceholder(LABEL_NO_MATCHING);
                    }
                    else
                    {
                        activateFilteringOptions(false);
                        listViewFiles.setPlaceholder(LABEL_NO_FITS);
                    }
                }
            }
            else
            {
                activateFilteringOptions(false);
                listViewFiles.setItems(null);
                listViewFiles.setPlaceholder(LABEL_NO_VALID_DIR);
                textFieldChooseDir.setStyle(INVALID_DIR_TEXTFIELD_STYLE);
            }
        }
        else
        {
            activateFilteringOptions(false);
            listViewFiles.setItems(null);
            listViewFiles.setPlaceholder(LABEL_NO_VALID_DIR);
            textFieldChooseDir.setStyle("");
        }
    }

    /**
     * Enables/Disables filtering option
     *
     * @param activate true for activating filtering option, false for deactivating
     */
    private void activateFilteringOptions(boolean activate)
    {
        labelFiltering.setDisable(!activate);
        labelFilteringFilename.setDisable(!activate);
        textFieldFilteringFilename.setDisable(!activate);
        buttonApplyFilter.setDisable(!activate);
        buttonDeselectAll.setDisable(!activate);
        buttonSelectAll.setDisable(!activate);
    }

    /**
     * Returns filtered FITS files from giving directory
     *
     * @param dir directory containing FITS files
     * @return filtered FITS files from giving directory
     */
    private List<String> listFiles(Path dir)
    {
        List<String> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter);)
        {
            for(Path file : stream)
            {
                if (Files.isRegularFile(file) && FitsFile.isFitsFile(file.toFile()))
                {
                    files.add(file.getFileName().toString());
                }
            }
        }
        catch (IOException exc)
        {
            return null;
        }
        return files;
    }

    /**
     * Sets reference to MainViewController (important for setting content in MainView)
     *
     * @param mainViewController MainViewController controller
     */
    public void setMainViewController(MainViewController mainViewController)
    {
        this.mainViewController = mainViewController;
    }

    /**
     * Initializes this controller - add listeners, set multiple selection mode and get user preferences
     */
    @FXML
    public void initialize()
    {
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
