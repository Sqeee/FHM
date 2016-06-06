package cz.muni.sci.astro.fhm.gui;

import cz.muni.sci.astro.fhm.core.MultipleOperationsRunner;
import cz.muni.sci.astro.fhm.core.Operation;
import cz.muni.sci.astro.fhm.core.OperationShift;
import cz.muni.sci.astro.fits.FitsCardDateValue;
import cz.muni.sci.astro.fits.FitsCardDateValueUnknownFormatException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
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
    private RandomAccessFile logFile;
    private MultipleOperationsRunner operationsRunner;
    private boolean allOK;

    @FXML
    private Button buttonExecute;
    @FXML
    private Button buttonExecuteQuit;
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
    private ListView<String> listViewFiles;
    @FXML
    private TextArea textAreaLog;
    @FXML
    private ListView<Operation> listViewOperationQueue;
    @FXML
    private Button buttonOperationQueueMoveUp;
    @FXML
    private Button buttonOperationQueueMoveDown;
    @FXML
    private Button buttonOperationQueueRemove;
    @FXML
    private Button buttonOperationQueueShow;
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
     * Prepares this form
     *
     * @param files              file list to working with
     * @param mainViewController MainViewController controller
     */
    public void prepareWindow(List<String> files, MainViewController mainViewController) {
        this.mainViewController = mainViewController;
        try {
            logFile = new RandomAccessFile(new File(LOG_FILE), "rw");
            logFile.setLength(0);
        } catch (IOException exc) {
            textAreaLog.setText(LOGGER_FAILED_TEXT);
            closeLogs();
        }
        operationsRunner = new MultipleOperationsRunner(this::printOK, this::printError);
        GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_FILE, MenuBarController.MENU_ITEM_FILE_SAVE, false, e -> handleClickButtonExecute());
        GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_FILE, MenuBarController.MENU_ITEM_FILE_EXIT, false, e -> quitWithConfirmation());
        GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_MODES, MenuBarController.MENU_ITEM_MODES_SINGLE, false, e -> switchMode());
        GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_MODES, MenuBarController.MENU_ITEM_MODES_MULTIPLE, true, null);
        if (files.isEmpty()) {
            buttonSelectAll.setDisable(true);
            buttonExecute.setDisable(true);
            buttonExecuteQuit.setDisable(true);
        }
        listViewFiles.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(new File(item).getName());
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
        listViewFiles.setItems(FXCollections.observableArrayList(files));
        listViewFiles.getSelectionModel().selectAll();
        listViewOperationQueue.setPlaceholder(new Label("No queued operations"));
        listViewOperationQueue.getSelectionModel().selectedItemProperty().addListener((ov, before, after) -> {
            buttonOperationQueueRemove.setDisable(after == null);
            buttonOperationQueueShow.setDisable(after == null);
        });
        listViewOperationQueue.getSelectionModel().selectedIndexProperty().addListener((ov, before, after) -> {
            GUIHelpers.checkMoveButtons(listViewOperationQueue.getSelectionModel().getSelectedIndex(), listViewOperationQueue.getItems().size(), buttonOperationQueueMoveUp, buttonOperationQueueMoveDown);
        });
        listViewOperationQueue.setItems(FXCollections.observableList(operationsRunner.getOperations()));
        listViewConcatenationValues.setPlaceholder(new Label("No values"));
        listViewConcatenationValues.getSelectionModel().selectedItemProperty().addListener((ov, before, after) -> {
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
        EventHandler<KeyEvent> ignoreEnter = keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                keyEvent.consume();
            }
        };
        textFieldAddCardKeyword.setOnKeyPressed(ignoreEnter);
        textFieldAddCardRValue.setOnKeyPressed(ignoreEnter);
        textFieldAddCardIValue.setOnKeyPressed(ignoreEnter);
        textFieldAddCardComment.setOnKeyPressed(ignoreEnter);
        textFieldAddCardIndex.setOnKeyPressed(ignoreEnter);
        textFieldAddCardAfterKeyword.setOnKeyPressed(ignoreEnter);
        textFieldRemoveCardKeyword.setOnKeyPressed(ignoreEnter);
        textFieldRemoveCardIndex.setOnKeyPressed(ignoreEnter);
        textFieldChangeCardKeyword.setOnKeyPressed(ignoreEnter);
        textFieldChangeCardNewKeyword.setOnKeyPressed(ignoreEnter);
        textFieldChangeCardRValue.setOnKeyPressed(ignoreEnter);
        textFieldChangeCardIValue.setOnKeyPressed(ignoreEnter);
        textFieldChangeCardComment.setOnKeyPressed(ignoreEnter);
        textFieldChangeCardIndex.setOnKeyPressed(ignoreEnter);
        textFieldConcatenateKeyword.setOnKeyPressed(ignoreEnter);
        textFieldShiftKeyword.setOnKeyPressed(ignoreEnter);
        textFieldShiftYears.setOnKeyPressed(ignoreEnter);
        textFieldShiftMonths.setOnKeyPressed(ignoreEnter);
        textFieldShiftDays.setOnKeyPressed(ignoreEnter);
        textFieldShiftHours.setOnKeyPressed(ignoreEnter);
        textFieldShiftMinutes.setOnKeyPressed(ignoreEnter);
        textFieldShiftSeconds.setOnKeyPressed(ignoreEnter);
        textFieldShiftMilliseconds.setOnKeyPressed(ignoreEnter);
        textFieldShiftMicroseconds.setOnKeyPressed(ignoreEnter);
        textFieldJDKeyword.setOnKeyPressed(ignoreEnter);
        textFieldJDSourceKeyword.setOnKeyPressed(ignoreEnter);
        textFieldJDSourceDatetime.setOnKeyPressed(ignoreEnter);
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
     * Handles click on button Execute - tries to execute operations, if there are some problems, shows them and return false
     *
     * @return true if save does not failed, otherwise false
     */
    @FXML
    private boolean handleClickButtonExecute() {
        boolean result;
        printOK();
        printOK("Executing operations on files.");
        allOK = true;
        operationsRunner.executeOperations();
        listViewOperationQueue.setItems(null);
        result = allOK;
        refreshLogs();
        if (result) {
            GUIHelpers.showAlert(AlertType.INFORMATION, "Executing operations", "Operations on all files were executed successfully.", null);
        } else {
            GUIHelpers.showAlert(AlertType.ERROR, "Executing operations", "Problems with executing operations.", "Details are in log.");
        }
        return result;
    }

    /**
     * Handles click on button Execute and quit - tries to execute operations, if there are some problems, shows them, in case of no problems quits
     */
    @FXML
    private void handleClickButtonExecuteQuit() {
        if (handleClickButtonExecute()) {
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
        for (int i = 0; i < listViewFiles.getItems().size(); i++) {
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
        printOperationAddQueueStartInfo("Preparing operation of adding new card:");
        operationsRunner.addCard(listViewFiles.getSelectionModel().getSelectedItems(), keyword, rValue, iValue, comment, index, afterKeyword, update);
        printOperationAddQueueEndInfo();
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
        printOperationAddQueueStartInfo("Preparing operation of deleting card:");
        operationsRunner.removeCard(listViewFiles.getSelectionModel().getSelectedItems(), keyword, index);
        printOperationAddQueueEndInfo();
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
        printOperationAddQueueStartInfo("Preparing operation of changing new card:");
        if (checkBoxChangeCardIndex.isSelected()) {
            operationsRunner.changeIndexCard(listViewFiles.getSelectionModel().getSelectedItems(), keyword, index);
        }
        if (checkBoxChangeCardNewKeyword.isSelected() || checkBoxChangeCardRValue.isSelected() || checkBoxChangeCardIValue.isSelected() || checkBoxChangeCardComment.isSelected()) {
            operationsRunner.changeCard(listViewFiles.getSelectionModel().getSelectedItems(), keyword, newKeyword, rValue, iValue, comment, delete);
        }
        printOperationAddQueueEndInfo();
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
        printOperationAddQueueStartInfo("Preparing operation of concatenating values:");
        operationsRunner.concatenate(listViewFiles.getSelectionModel().getSelectedItems(), keyword, values, "", update);
        printOperationAddQueueEndInfo();
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
        moveSelectedConcatenationValue(1);
        listViewConcatenationValues.getSelectionModel().selectNext();
    }

    /**
     * Handles click on button move up - moves up concatenated values
     */
    @FXML
    private void handleClickButtonConcatenateMoveUp() {
        moveSelectedConcatenationValue(-1);
        listViewConcatenationValues.getSelectionModel().selectPrevious();
    }

    /**
     * Moves selected value in list view with given offset
     *
     * @param offset how many lines values should be moved
     */
    private void moveSelectedConcatenationValue(int offset) {
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
                intervals.add(OperationShift.PREFIX_YEAR + interval);
            }
        }
        value = textFieldShiftMonths.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Months");
            if (interval == null) {
                return;
            } else {
                intervals.add(OperationShift.PREFIX_MONTH + interval);
            }
        }
        value = textFieldShiftDays.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Days");
            if (interval == null) {
                return;
            } else {
                intervals.add(OperationShift.PREFIX_DAY + interval);
            }
        }
        value = textFieldShiftHours.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Hours");
            if (interval == null) {
                return;
            } else {
                intervals.add(OperationShift.PREFIX_HOUR + interval);
            }
        }
        value = textFieldShiftMinutes.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Minutes");
            if (interval == null) {
                return;
            } else {
                intervals.add(OperationShift.PREFIX_MINUTE + interval);
            }
        }
        value = textFieldShiftSeconds.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Seconds");
            if (interval == null) {
                return;
            } else {
                intervals.add(OperationShift.PREFIX_SECOND + interval);
            }
        }
        value = textFieldShiftMilliseconds.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Milliseconds");
            if (interval == null) {
                return;
            } else {
                intervals.add(OperationShift.PREFIX_MILLISECOND + interval);
            }
        }
        value = textFieldShiftMicroseconds.getText().trim();
        if (!value.isEmpty()) {
            interval = parseInt(value, "Microseconds");
            if (interval == null) {
                return;
            } else {
                intervals.add(OperationShift.PREFIX_MICROSECOND + interval);
            }
        }
        if (intervals.isEmpty()) {
            GUIHelpers.showAlert(AlertType.INFORMATION, "Shifting", "At least one time parameter must be specified", "");
            return;
        }
        printOperationAddQueueStartInfo("Preparing operation of shifting values:");
        operationsRunner.shift(listViewFiles.getSelectionModel().getSelectedItems(), keyword, intervals);
        printOperationAddQueueEndInfo();
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
        printOperationAddQueueStartInfo("Preparing operation of adding card with julian day:");
        operationsRunner.jd(listViewFiles.getSelectionModel().getSelectedItems(), keyword, sourceKeyword, dateTime, update);
        printOperationAddQueueEndInfo();
    }

    /**
     * Handles click on button move down - moves down operation
     */
    @FXML
    private void handleClickButtonOperationQueueMoveDown() {
        moveSelectedOperation(1);
    }

    /**
     * Handles click on button move up - moves up operation
     */
    @FXML
    private void handleClickButtonOperationQueueMoveUp() {
        moveSelectedOperation(-1);
    }

    /**
     * Moves selected operation in list view with given offset
     *
     * @param offset how many lines operation should be moved
     */
    private void moveSelectedOperation(int offset) {
        int selectedIndex = listViewOperationQueue.getSelectionModel().getSelectedIndex();
        operationsRunner.swapOperations(listViewOperationQueue.getItems().get(selectedIndex), listViewOperationQueue.getItems().get(selectedIndex + offset));
        listViewOperationQueue.setItems(FXCollections.observableList(operationsRunner.getOperations()));
    }

    /**
     * Handles click on button remove - removes operation
     */
    @FXML
    private void handleClickButtonOperationQueueRemove() {
        Operation operation = listViewOperationQueue.getSelectionModel().getSelectedItem();
        operationsRunner.removeOperation(operation);
        listViewOperationQueue.setItems(FXCollections.observableList(operationsRunner.getOperations()));
    }

    /**
     * Handles click on button show - show files affected by selected operation
     */
    @FXML
    private void handleClickButtonOperationQueueShowFiles() {
        StringJoiner joiner = new StringJoiner(", ");
        List<String> files = operationsRunner.getFilesForOperation(listViewOperationQueue.getSelectionModel().getSelectedItem());
        for (String file : files) {
            joiner.add(new File(file).getName());
        }
        GUIHelpers.showAlert(AlertType.INFORMATION, "Affected files by operation", "These files will be affected by operation", joiner.toString());
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
        Platform.exit();
        System.exit(0);
    }

    /**
     * Shows confirmation of exit (if it is necessary) and then maybe closes entire app
     */
    @FXML
    private void quitWithConfirmation() {
        if (operationsRunner.getOperations().isEmpty()) {
            quit();
            return;
        }
        GUIHelpers.showAlert(AlertType.CONFIRMATION, "Confirm exit", "Exiting application", "Are you sure you want to exit application? You lost all not executed operations.").ifPresent(response -> {
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
     * Prints info about start of adding operation into queue
     *
     * @param text info
     */
    private void printOperationAddQueueStartInfo(String text) {
        allOK = true;
        printOK(text);
    }

    /**
     * Prints operation info about end of adding operation into queue - show success and refreshes logs
     */
    private void printOperationAddQueueEndInfo() {
        printOK();
        refreshLogs();
        listViewOperationQueue.setItems(FXCollections.observableList(operationsRunner.getOperations()));
        if (allOK) {
            GUIHelpers.showAlert(AlertType.INFORMATION, "Operation summary", "Adding operation into queue was completed without errors.", "");
        } else {
            GUIHelpers.showAlert(AlertType.ERROR, "Operation summary", "Some errors occur during adding last operation into queue. Check logs.", "");
        }
    }

    /**
     * Switches mode to single operation
     */
    private void switchMode() {
        if (listViewFiles.getItems().size() > 50) {
            GUIHelpers.showAlert(AlertType.INFORMATION, "Switch to single operation mode", "Switching cannot be done", "Single operation mode is not for too much open files, it was intended only for few files, where you want exact editing and WYSIWYG editor.");
            return;
        }
        if (operationsRunner.getOperations().isEmpty()) {
            openEditFrom();
            return;
        }
        GUIHelpers.showAlert(AlertType.CONFIRMATION, "Confirm switching mode", "Switching modes", "Are you sure you want to switch modes? You lost all not executed operations.").ifPresent(response -> {
            if (response == ButtonType.OK) {
                EditController editController = (EditController) mainViewController.setContent("fxml/Edit.fxml");
                closeLogs();
                editController.prepareWindow(listViewFiles.getItems(), mainViewController);
            }
        });

    }

    /**
     * Opens single edit form
     */
    private void openEditFrom() {
        EditController editController = (EditController) mainViewController.setContent("fxml/Edit.fxml");
        closeLogs();
        editController.prepareWindow(listViewFiles.getItems(), mainViewController);
    }
}
