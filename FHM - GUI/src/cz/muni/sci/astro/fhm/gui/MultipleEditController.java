package cz.muni.sci.astro.fhm.gui;

import cz.muni.sci.astro.fhm.core.Helpers;
import cz.muni.sci.astro.fhm.core.MultipleOperationsRunner;
import cz.muni.sci.astro.fits.FitsCardDateValue;
import cz.muni.sci.astro.fits.FitsCardDateValueUnknownFormatException;
import cz.muni.sci.astro.fits.FitsException;
import cz.muni.sci.astro.fits.FitsFile;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for multiple editing cards
 *
 * @author Jan Hlava, 395986
 */
public class MultipleEditController {
    private static final String LOG_FILE = "multiple_operations.log";
    private static final String LOGGER_FAILED_TEXT = "Logger failed. No log records will be available.";
    private static final long MAX_DISPLAYED_CHARS = 100000;
    private MainViewController mainViewController;
    private List<String> files;
    private List<FitsFile> fitsFiles;
    private String dir;
    private RandomAccessFile logFile;
    private MultipleOperationsRunner operationsRunner;
    private boolean allOK;

    @FXML
    private Button buttonSave;
    @FXML
    private Button buttonSaveQuit;
    @FXML
    private Button buttonDeselectAll;
    @FXML
    private Button buttonSelectAll;
    @FXML
    private Button buttonInvertSelection;
    @FXML
    private Button buttonAddCard;
    @FXML
    private Button buttonRemoveCard;
    @FXML
    private Button buttonChangeCard;
    @FXML
    private Button buttonConcatenate;
    @FXML
    private Button buttonShift;
    @FXML
    private Button buttonAddJD;
    @FXML
    private ListView<FitsFile> listViewFiles;
    @FXML
    private TextArea textAreaLog;
    @FXML
    private TextField textFieldAddCardKeyword;
    @FXML
    private TextField textFieldAddCardRValue;
    @FXML
    private TextField textFieldAddCardIValue;
    @FXML
    private TextField textFieldAddCardComment;
    @FXML
    private TextField textFieldAddCardIndex;
    @FXML
    private TextField textFieldAddCardAfterKeyword;
    @FXML
    private CheckBox checkBoxAddCardRValue;
    @FXML
    private CheckBox checkBoxAddCardIValue;
    @FXML
    private CheckBox checkBoxAddCardComment;
    @FXML
    private CheckBox checkBoxAddCardPosition;
    @FXML
    private CheckBox checkBoxAddCardUpdate;
    @FXML
    private Toggle toggleAddCardIndex;
    @FXML
    private Toggle toggleAddCardAfterCard;
    @FXML
    private ToggleGroup toggleGroupAddCardPosition;
    @FXML
    private TextField textFieldRemoveCardKeyword;
    @FXML
    private TextField textFieldRemoveCardIndex;
    @FXML
    private Toggle toggleRemoveCardKeyword;
    @FXML
    private Toggle toggleRemoveCardIndex;
    @FXML
    private ToggleGroup toggleGroupRemoveCard;
    @FXML
    private TextField textFieldChangeCardKeyword;
    @FXML
    private TextField textFieldChangeCardNewKeyword;
    @FXML
    private TextField textFieldChangeCardRValue;
    @FXML
    private TextField textFieldChangeCardIValue;
    @FXML
    private TextField textFieldChangeCardComment;
    @FXML
    private TextField textFieldChangeCardIndex;
    @FXML
    private CheckBox checkBoxChangeCardNewKeyword;
    @FXML
    private CheckBox checkBoxChangeCardRValue;
    @FXML
    private CheckBox checkBoxChangeCardIValue;
    @FXML
    private CheckBox checkBoxChangeCardComment;
    @FXML
    private CheckBox checkBoxChangeCardIndex;
    @FXML
    private CheckBox checkBoxChangeCardDelete;
    @FXML
    private TextField textFieldConcatenateKeyword;
    @FXML
    private TextField textFieldConcatenateKeywordValue;
    @FXML
    private TextField textFieldConcatenateString;
    @FXML
    private ListView<ConcatenateValue> listViewConcatenationValues;
    @FXML
    private CheckBox checkBoxConcatenateUpdate;
    @FXML
    private Button buttonConcatenateAddKeywordValue;
    @FXML
    private Button buttonConcatenateAddString;
    @FXML
    private Button buttonConcatenateRemove;
    @FXML
    private Button buttonConcatenateMoveUp;
    @FXML
    private Button buttonConcatenateMoveDown;
    @FXML
    private TextField textFieldShiftKeyword;
    @FXML
    private TextField textFieldShiftYears;
    @FXML
    private TextField textFieldShiftMonths;
    @FXML
    private TextField textFieldShiftDays;
    @FXML
    private TextField textFieldShiftHours;
    @FXML
    private TextField textFieldShiftMinutes;
    @FXML
    private TextField textFieldShiftSeconds;
    @FXML
    private TextField textFieldShiftMilliseconds;
    @FXML
    private TextField textFieldShiftMicroseconds;
    @FXML
    private TextField textFieldJDKeyword;
    @FXML
    private TextField textFieldJDSourceKeyword;
    @FXML
    private TextField textFieldJDSourceDatetime;
    @FXML
    private CheckBox checkBoxJDUpdate;
    @FXML
    private Toggle toggleJDSourceKeyword;
    @FXML
    private Toggle toggleJDSourceDatetime;
    @FXML
    private ToggleGroup toggleGroupJD;

    /**
     * Sets reference to MainViewController (important for setting content in MainView)
     *
     * @param mainViewController MainViewController controller
     */
    public void setMainViewController(MainViewController mainViewController) {
        this.mainViewController = mainViewController;
    }

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
     * Stores opened fits files
     *
     * @param fitsFiles list of opened fits files
     */
    public void setFitsFiles(List<FitsFile> fitsFiles) {
        this.fitsFiles = fitsFiles;
    }

    /**
     * Prepares this form
     */
    public void prepareWindow() {
        try {
            logFile = new RandomAccessFile(new File(LOG_FILE), "rw");
            logFile.setLength(0);
        } catch (IOException exc) {
            textAreaLog.setText(LOGGER_FAILED_TEXT);
            closeLogs();
        }
        operationsRunner = new MultipleOperationsRunner(this::printOK, this::printError);
        GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_FILE, MenuBarController.MENU_ITEM_FILE_SAVE, false, e -> handleClickButtonSave());
        GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_FILE, MenuBarController.MENU_ITEM_FILE_EXIT, false, e -> quitWithConfirmation());
        GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_MODES, MenuBarController.MENU_ITEM_MODES_SINGLE, false, e -> switchMode());
        GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_MODES, MenuBarController.MENU_ITEM_MODES_MULTIPLE, true, null);
        if (fitsFiles == null) {
            fitsFiles = new ArrayList<>(files.size());
            List<String> errors = new ArrayList<>();
            for (String filename : files) {
                try {
                    fitsFiles.add(new FitsFile(new File(dir + '/' + filename)));
                } catch (FitsException exc) {
                    errors.add("Error: cannot load file " + filename + " with reason " + exc.getMessage());
                }
            }
            if (!errors.isEmpty()) {
                GUIHelpers.showAlert(AlertType.ERROR, "Error", "Error occurred", String.join("\n", errors));
            }
            if (fitsFiles.isEmpty()) {
                buttonSelectAll.setDisable(true);
                buttonSave.setDisable(true);
                buttonSaveQuit.setDisable(true);
            }
        }
        listViewFiles.setCellFactory(listView -> new ListCell<FitsFile>() {
            @Override
            protected void updateItem(FitsFile item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(item.getFilename());
                }
            }
        });
        listViewFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listViewFiles.getSelectionModel().selectedItemProperty().addListener((ov, before, after) -> {
            buttonDeselectAll.setDisable(after == null);
            buttonInvertSelection.setDisable(after == null);
            buttonAddCard.setDisable(after == null);
            buttonRemoveCard.setDisable(after == null);
            buttonChangeCard.setDisable(after == null);
            buttonConcatenate.setDisable(after == null);
            buttonShift.setDisable(after == null);
            buttonAddJD.setDisable(after == null);
        });
        listViewFiles.setPlaceholder(new Label("No files"));
        listViewFiles.setItems(FXCollections.observableArrayList(fitsFiles));
        listViewFiles.getSelectionModel().selectAll();
        listViewConcatenationValues.setPlaceholder(new Label("No values"));
        listViewConcatenationValues.getSelectionModel().selectedItemProperty().addListener((ov, before, after) -> {
            buttonConcatenateMoveUp.setDisable(after == null);
            buttonConcatenateMoveDown.setDisable(after == null);
            buttonConcatenateRemove.setDisable(after == null);
        });
        listViewConcatenationValues.getSelectionModel().selectedIndexProperty().addListener((ov, before, after) -> {
            GUIHelpers.checkMoveButtons(listViewConcatenationValues.getSelectionModel().getSelectedIndex(), listViewConcatenationValues.getItems().size(), buttonConcatenateMoveUp, buttonConcatenateMoveDown);
        });
        textFieldConcatenateKeywordValue.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                buttonConcatenateAddKeywordValue.fire();
                keyEvent.consume();
            }
        });
        textFieldConcatenateString.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                buttonConcatenateAddString.fire();
                keyEvent.consume();
            }
        });
        textFieldAddCardRValue.textProperty().addListener((observable, oldValue, newValue) -> checkBoxAddCardRValue.setSelected(true));
        textFieldAddCardIValue.textProperty().addListener((observable, oldValue, newValue) -> checkBoxAddCardIValue.setSelected(true));
        textFieldAddCardComment.textProperty().addListener((observable, oldValue, newValue) -> checkBoxAddCardComment.setSelected(true));
        textFieldAddCardIndex.textProperty().addListener((observable, oldValue, newValue) -> {
            checkBoxAddCardPosition.setSelected(true);
            toggleAddCardIndex.setSelected(true);
        });
        textFieldAddCardAfterKeyword.textProperty().addListener((observable, oldValue, newValue) -> {
            checkBoxAddCardPosition.setSelected(true);
            toggleAddCardAfterCard.setSelected(true);
        });
        textFieldRemoveCardKeyword.textProperty().addListener((observable, oldValue, newValue) -> toggleRemoveCardKeyword.setSelected(true));
        textFieldRemoveCardIndex.textProperty().addListener((observable, oldValue, newValue) -> toggleRemoveCardIndex.setSelected(true));
        textFieldChangeCardNewKeyword.textProperty().addListener((observable, oldValue, newValue) -> checkBoxChangeCardNewKeyword.setSelected(true));
        textFieldChangeCardRValue.textProperty().addListener((observable, oldValue, newValue) -> checkBoxChangeCardRValue.setSelected(true));
        textFieldChangeCardIValue.textProperty().addListener((observable, oldValue, newValue) -> checkBoxChangeCardIValue.setSelected(true));
        textFieldChangeCardComment.textProperty().addListener((observable, oldValue, newValue) -> checkBoxChangeCardComment.setSelected(true));
        textFieldChangeCardIndex.textProperty().addListener((observable, oldValue, newValue) -> checkBoxChangeCardIndex.setSelected(true));
        textFieldJDSourceKeyword.textProperty().addListener((observable, oldValue, newValue) -> toggleJDSourceKeyword.setSelected(true));
        textFieldJDSourceDatetime.textProperty().addListener((observable, oldValue, newValue) -> toggleJDSourceDatetime.setSelected(true));
    }

    /**
     * Handles click on button Save - tries to save content, if there are some problems, shows them and return false
     *
     * @return true if save does not failed, otherwise false
     */
    @FXML
    private boolean handleClickButtonSave() {
        List<String> problems = new ArrayList<>();
        for (FitsFile file : fitsFiles) {
            List<String> problemsFile = file.getHDU(0).getHeader().checkSaveHeader();
            if (!problemsFile.isEmpty()) {
                problems.add("Saving file " + file.getFilename() + " fails with error: " + String.join(System.lineSeparator(), problemsFile));
            }
        }
        List<String> notSavedFiles = new ArrayList<>();
        notSavedFiles.addAll(fitsFiles.stream().filter(file -> !file.saveFile()).map(FitsFile::getFilename).collect(Collectors.toList()));
        if (!problems.isEmpty()) {
            problems.add(0, "Saving these files failed: " + String.join(", ", notSavedFiles));
            problems.add(System.lineSeparator());
            printError(String.join(System.lineSeparator(), problems));
            refreshLogs();
            GUIHelpers.showAlert(AlertType.ERROR, "Saving files", "Problems with saving.", "Details are in log.");
            return false;
        }
        printOK("All files were saved successfully.");
        GUIHelpers.showAlert(AlertType.INFORMATION, "Saving files", "Files were saved successfully.", null);
        return true;
    }

    /**
     * Handles click on button Quit and save - tries to save content, if there are some problems, shows them, in case of no problems quits
     */
    @FXML
    private void handleClickButtonQuitSave() {
        if (handleClickButtonSave()) {
            quit();
        }
    }

    /**
     * Handles click on button Deselect all - deselects all files
     */
    @FXML
    private void handleClickButtonDeselectAll() {
        listViewFiles.getSelectionModel().clearSelection();
        listViewFiles.requestFocus();
    }

    /**
     * Handles click on button Select all - selects all files
     */
    @FXML
    private void handleClickButtonSelectAll() {
        listViewFiles.getSelectionModel().selectAll();
        listViewFiles.requestFocus();
    }

    /**
     * Handles click on button Invert selection - inverts selection
     */
    @FXML
    private void handleClickButtonInvertSelection() {
        for (int i = 0; i < fitsFiles.size(); i++) {
            if (listViewFiles.getSelectionModel().isSelected(i)) {
                listViewFiles.getSelectionModel().clearSelection(i);
            } else {
                listViewFiles.getSelectionModel().select(i);
            }
        }
        listViewFiles.requestFocus();
    }

    /**
     * Handles click on add card - add cards with given values
     */
    @FXML
    private void handleClickButtonAddCard() {
        String keyword = textFieldAddCardKeyword.getText();
        String rValue;
        if (checkBoxAddCardRValue.isSelected()) {
            rValue = textFieldAddCardRValue.getText();
        } else {
            rValue = null;
        }
        String iValue;
        if (checkBoxAddCardIValue.isSelected()) {
            iValue = textFieldAddCardIValue.getText();
        } else {
            iValue = null;
        }
        String comment;
        if (checkBoxAddCardComment.isSelected()) {
            comment = textFieldAddCardComment.getText();
        } else {
            comment = null;
        }
        Integer index = null;
        String afterKeyword = null;
        if (checkBoxAddCardPosition.isSelected()) {
            if (toggleGroupAddCardPosition.getSelectedToggle() == toggleAddCardIndex) {
                index = parseInt(textFieldAddCardIndex.getText(), "Index");
                if (index == null) {
                    return;
                }
            } else {
                afterKeyword = textFieldAddCardAfterKeyword.getText();
            }
        }
        boolean update = checkBoxAddCardUpdate.isSelected();
        printOperationStartInfo("Operation of adding new card:");
        operationsRunner.addCard(listViewFiles.getSelectionModel().getSelectedItems(), keyword, rValue, iValue, comment, index, afterKeyword, update);
        printOperationEndInfo();
    }

    /**
     * Handles click on remove card - remove cards with given values
     */
    @FXML
    private void handleClickButtonRemoveCard() {
        String keyword;
        Integer index;
        if (toggleGroupRemoveCard.getSelectedToggle() == toggleRemoveCardKeyword) {
            keyword = textFieldRemoveCardKeyword.getText();
            index = null;
        } else {
            keyword = null;
            index = parseInt(textFieldRemoveCardIndex.getText(), "Index");
            if (index == null) {
                return;
            }
        }
        printOperationStartInfo("Operation of deleting card:");
        operationsRunner.removeCard(listViewFiles.getSelectionModel().getSelectedItems(), keyword, index);
        printOperationEndInfo();
    }

    /**
     * Handles click on change card - change cards with given values
     */
    @FXML
    private void handleClickButtonChangeCard() {
        String keyword = textFieldChangeCardKeyword.getText();
        String newKeyword;
        if (checkBoxChangeCardNewKeyword.isSelected()) {
            newKeyword = textFieldChangeCardNewKeyword.getText();
        } else {
            newKeyword = null;
        }
        String rValue;
        if (checkBoxChangeCardRValue.isSelected()) {
            rValue = textFieldChangeCardRValue.getText();
        } else {
            rValue = null;
        }
        String iValue;
        if (checkBoxChangeCardIValue.isSelected()) {
            iValue = textFieldChangeCardIValue.getText();
        } else {
            iValue = null;
        }
        String comment;
        if (checkBoxChangeCardComment.isSelected()) {
            comment = textFieldChangeCardComment.getText();
        } else {
            comment = null;
        }
        Integer index = null;
        if (checkBoxChangeCardIndex.isSelected()) {
            index = parseInt(textFieldChangeCardIndex.getText(), "Index");
            if (index == null) {
                return;
            }
        }
        boolean delete = checkBoxChangeCardDelete.isSelected();
        printOperationStartInfo("Operation of changing new card:");
        if (checkBoxChangeCardIndex.isSelected()) {
            operationsRunner.changeIndexCard(listViewFiles.getSelectionModel().getSelectedItems(), keyword, index);
        }
        if (checkBoxChangeCardNewKeyword.isSelected() || checkBoxChangeCardRValue.isSelected() || checkBoxChangeCardIValue.isSelected() || checkBoxChangeCardComment.isSelected()) {
            operationsRunner.changeCard(listViewFiles.getSelectionModel().getSelectedItems(), keyword, newKeyword, rValue, iValue, comment, delete);
        }
        printOperationEndInfo();
    }

    /**
     * Handles click on concatenate - concatenate values
     */
    @FXML
    private void handleClickButtonConcatenate() {
        String keyword = textFieldConcatenateKeyword.getText();
        List<String> values = new ArrayList<>(listViewConcatenationValues.getItems().size());
        values.addAll(listViewConcatenationValues.getItems().stream().map(ConcatenateValue::toExport).collect(Collectors.toList()));
        boolean update = checkBoxConcatenateUpdate.isSelected();
        if (values.isEmpty()) {
            GUIHelpers.showAlert(AlertType.INFORMATION, "Concatenating", "At least one concatenation value must be inserted.", "");
            return;
        }
        printOperationStartInfo("Operation of concatenating values:");
        operationsRunner.concatenate(listViewFiles.getSelectionModel().getSelectedItems(), keyword, values, "", update);
        printOperationEndInfo();
    }

    /**
     * Handles click on button add keyword values - add keyword value to concatenation
     */
    @FXML
    private void handleClickButtonConcatenateAddKeywordValue() {
        ConcatenateValue value = new ConcatenateValue(ConcatenateType.KEYWORD_VALUE, textFieldConcatenateKeywordValue.getText());
        listViewConcatenationValues.getItems().add(value);
    }

    /**
     * Handles click on button add string values - add string value to concatenation
     */
    @FXML
    private void handleClickButtonConcatenateAddString() {
        ConcatenateValue value = new ConcatenateValue(ConcatenateType.STRING, textFieldConcatenateString.getText());
        listViewConcatenationValues.getItems().add(value);
    }

    /**
     * Handles click on button move down - moves down concatenated values
     */
    @FXML
    private void handleClickButtonConcatenateMoveDown() {
        moveSelectedValue(1);
        listViewConcatenationValues.getSelectionModel().selectNext();
    }

    /**
     * Handles click on button move up - moves up concatenated values
     */
    @FXML
    private void handleClickButtonConcatenateMoveUp() {
        moveSelectedValue(-1);
        listViewConcatenationValues.getSelectionModel().selectPrevious();
    }

    /**
     * Moves selected value in list view with given offset
     *
     * @param offset how many lines values should be moved
     */
    private void moveSelectedValue(int offset) {
        int selectedIndex = listViewConcatenationValues.getSelectionModel().getSelectedIndex();
        ConcatenateValue movedValue = listViewConcatenationValues.getItems().set(selectedIndex + offset, listViewConcatenationValues.getSelectionModel().getSelectedItem());
        listViewConcatenationValues.getItems().set(selectedIndex, movedValue);
    }

    /**
     * Handles click on button remove - removes concatenated value
     */
    @FXML
    private void handleClickButtonConcatenateRemove() {
        listViewConcatenationValues.getItems().remove(listViewConcatenationValues.getSelectionModel().getSelectedIndex());
    }

    /**
     * Handles click on add JD - add cards with julian day
     */
    @FXML
    private void handleClickButtonShift() {
        String keyword = textFieldShiftKeyword.getText();
        List<String> intervals = new ArrayList<>();
        Integer interval;
        String value = textFieldShiftYears.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Years");
            if (interval == null) {
                return;
            } else {
                intervals.add(MultipleOperationsRunner.PARAM_SHIFTING_YEAR_PREFIX + interval);
            }
        }
        value = textFieldShiftMonths.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Months");
            if (interval == null) {
                return;
            } else {
                intervals.add(MultipleOperationsRunner.PARAM_SHIFTING_MONTH_PREFIX + interval);
            }
        }
        value = textFieldShiftDays.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Days");
            if (interval == null) {
                return;
            } else {
                intervals.add(MultipleOperationsRunner.PARAM_SHIFTING_DAY_PREFIX + interval);
            }
        }
        value = textFieldShiftHours.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Hours");
            if (interval == null) {
                return;
            } else {
                intervals.add(MultipleOperationsRunner.PARAM_SHIFTING_HOUR_PREFIX + interval);
            }
        }
        value = textFieldShiftMinutes.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Minutes");
            if (interval == null) {
                return;
            } else {
                intervals.add(MultipleOperationsRunner.PARAM_SHIFTING_MINUTE_PREFIX + interval);
            }
        }
        value = textFieldShiftSeconds.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Seconds");
            if (interval == null) {
                return;
            } else {
                intervals.add(MultipleOperationsRunner.PARAM_SHIFTING_SECOND_PREFIX + interval);
            }
        }
        value = textFieldShiftMilliseconds.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Milliseconds");
            if (interval == null) {
                return;
            } else {
                intervals.add(MultipleOperationsRunner.PARAM_SHIFTING_MILLISECOND_PREFIX + interval);
            }
        }
        value = textFieldShiftMicroseconds.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Microseconds");
            if (interval == null) {
                return;
            } else {
                intervals.add(MultipleOperationsRunner.PARAM_SHIFTING_MICROSECOND_PREFIX + interval);
            }
        }
        if (intervals.isEmpty()) {
            GUIHelpers.showAlert(AlertType.INFORMATION, "Shifting", "At least one time parameter must be specified", "");
            return;
        }
        printOperationStartInfo("Operation of shifting values:");
        operationsRunner.shift(listViewFiles.getSelectionModel().getSelectedItems(), keyword, intervals);
        printOperationEndInfo();
    }

    /**
     * Handles click on add JD - add cards with julian day
     */
    @FXML
    private void handleClickButtonAddJD() {
        String keyword = textFieldJDKeyword.getText();
        String sourceKeyword;
        String dateTime;
        if (toggleGroupJD.getSelectedToggle() == toggleJDSourceKeyword) {
            sourceKeyword = textFieldJDSourceKeyword.getText();
            dateTime = null;
        } else {
            sourceKeyword = null;
            dateTime = textFieldJDSourceDatetime.getText();
            try {
                FitsCardDateValue.createFromDateString(dateTime);
            } catch (FitsCardDateValueUnknownFormatException exc) {
                GUIHelpers.showAlert(AlertType.ERROR, "Invalid input", "Date time has unknown date time format", "");
                return;
            }
        }
        boolean update = checkBoxJDUpdate.isSelected();
        printOperationStartInfo("Operation of adding card with julian day:");
        operationsRunner.jd(listViewFiles.getSelectionModel().getSelectedItems(), keyword, sourceKeyword, dateTime, update);
        printOperationEndInfo();
    }

    /**
     * Refreshes log viewer
     */
    private void refreshLogs() {
        try {
            byte[] bytes = new byte[(int) MAX_DISPLAYED_CHARS];
            logFile.seek(Math.max(logFile.length() - MAX_DISPLAYED_CHARS, 0l));
            int readChars = logFile.read(bytes);
            if (readChars != -1) {
                textAreaLog.setText((new String(bytes)).trim());
            } else {
                textAreaLog.setText("(Empty)");
            }
            textAreaLog.end();
        } catch (IOException exc) {
            textAreaLog.setText(LOGGER_FAILED_TEXT);
        }
    }

    /**
     * Closes logs
     */
    public void closeLogs() {
        if (logFile != null) {
            try {
                logFile.close();
            } catch (IOException ignored) {
            } // I want log read to close, but if not, so what i can do with that - nothing
        }
    }

    /**
     * Closes entire app
     */
    private void quit() {
        closeLogs();
        fitsFiles.forEach(FitsFile::closeFile);
        Platform.exit();
        System.exit(0);
    }

    /**
     * Shows confirmation of exit and then maybe closes entire app
     */
    @FXML
    private void quitWithConfirmation() {
        GUIHelpers.showAlert(AlertType.CONFIRMATION, "Confirm exit", "Exiting application", "Are you sure you want to exit application? You lost all unsaved changes.").ifPresent(response -> {
            if (response == ButtonType.OK) {
                quit();
            }
        });
    }

    /**
     * Prints message to output
     *
     * @param partsMessage parts of message which should be printed
     */
    private void printOK(String... partsMessage) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String part : partsMessage) {
            stringBuilder.append(part);
        }
        stringBuilder.append(System.lineSeparator());
        try {
            logFile.write(stringBuilder.toString().getBytes());
        } catch (IOException ignored) // Truncate logs
        {
        }
    }

    /**
     * Prints error message to output
     *
     * @param partsMessage parts of message which should be printed
     */
    private void printError(String... partsMessage) {
        allOK = false;
        StringBuilder stringBuilder = new StringBuilder("ERROR: ");
        for (String part : partsMessage) {
            stringBuilder.append(part);
        }
        stringBuilder.append(System.lineSeparator());
        try {
            logFile.write(stringBuilder.toString().getBytes());
        } catch (IOException ignored) // Truncate logs
        {
        }
    }

    /**
     * Try parse number from textField
     *
     * @param number        number to parse
     * @param textFieldName name of textfield used for error alert
     * @return parsed number or null in case of parsing failure
     */
    private Integer parseInt(String number, String textFieldName) {
        Integer result = null;
        try {
            result = Integer.parseInt(number);
        } catch (NumberFormatException exc) {
            GUIHelpers.showAlert(AlertType.ERROR, "Invalid input", textFieldName + " is not valid integer", "");
        }
        return result;
    }

    /**
     * Prints info about start of operation
     *
     * @param text info
     */
    private void printOperationStartInfo(String text) {
        allOK = true;
        printOK(text);
    }

    /**
     * Prints operation info about success and refreshes logs
     */
    private void printOperationEndInfo() {
        printOK();
        refreshLogs();
        if (allOK) {
            GUIHelpers.showAlert(AlertType.INFORMATION, "Operation summary", "Operation was completed without errors", "");
        } else {
            GUIHelpers.showAlert(AlertType.ERROR, "Operation summary", "Some error occurs during processing last operation. Check logs.", "");
        }
    }

    /**
     * Switches mode to single operation
     */
    private void switchMode() {
        if (fitsFiles.size() > 50) {
            GUIHelpers.showAlert(AlertType.INFORMATION, "Swith to single operation mode", "Switching cannot be done", "Single operation mode is not for too much open files, it was intended only for few files, where you want exact editing and WYSIWYG editor.");
            return;
        }
        EditController editController = (EditController) mainViewController.setContent("Edit.fxml");
        editController.setMainViewController(mainViewController);
        editController.setFitsFiles(fitsFiles);
        closeLogs();
        editController.prepareWindow();
    }
}
