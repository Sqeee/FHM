package cz.muni.sci.astro.fhm.gui;

import cz.muni.sci.astro.fits.FitsCard;
import cz.muni.sci.astro.fits.FitsFile;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.util.Pair;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for concatenation manager
 *
 * @author Jan Hlava, 395986
 */
public class ConcatenationManagerController {
    private static final String VALUE_SEPARATOR = " -> ";
    private static final String ADD_NEW = "(Add new)";

    private Stage stage;
    private boolean useConcatenation;

    @FXML
    private ListView<FitsCard> listViewCardValues;
    @FXML
    private ListView<String> listViewConcatenation;
    @FXML
    private Button buttonCardValuesAdd;
    @FXML
    private Button buttonCustomValueAdd;
    @FXML
    private Button buttonCopyValue;
    @FXML
    private Button buttonRemove;
    @FXML
    private Button buttonMoveUp;
    @FXML
    private Button buttonMoveDown;
    @FXML
    private TextField textFieldCustomValue;
    @FXML
    private TextField textFieldGlue;
    @FXML
    private ChoiceBox<FitsCard> choiceBoxTarget;

    /**
     * Prepares this dialog
     *
     * @param cards        cards for setting possible values and target
     * @param cardsInFiles map containing information in which file card is stored
     * @param selectedCard selected card
     * @param showFiles    should be shown filenames?
     * @param stage        stage of this dialog
     */
    public void prepareWindow(List<FitsCard> cards, Map<FitsCard, FitsFile> cardsInFiles, FitsCard selectedCard, boolean showFiles, Stage stage) {
        this.stage = stage;
        StringConverter<FitsCard> choiceBoxTargetStringConverter = new StringConverter<FitsCard>() {
            @Override
            public String toString(FitsCard object) {
                if (object == null) {
                    return ADD_NEW;
                }
                if (showFiles) {
                    return cardsInFiles.get(object).getFilename();
                } else {
                    String keywordName = object.getKeywordName();
                    if (keywordName.trim().isEmpty()) {
                        keywordName = "(EMPTY)";
                    }
                    return keywordName;
                }
            }

            @Override
            public FitsCard fromString(String string) {
                return null; // I do not need this method
            }
        };
        choiceBoxTarget.setConverter(choiceBoxTargetStringConverter);
        choiceBoxTarget.getItems().add(null);
        choiceBoxTarget.getItems().addAll(cards);
        choiceBoxTarget.getItems().sort((i1, i2) -> {
            if (i1 == null) {
                return -1;
            } else if (i2 == null) {
                return 1;
            }
            return i1.getKeywordName().compareTo(i2.getKeywordName());
        });
        if (selectedCard != null) {
            choiceBoxTarget.getSelectionModel().select(selectedCard);
        } else {
            choiceBoxTarget.getSelectionModel().select(0);
        }
        listViewCardValues.setCellFactory(listView -> new ListCell<FitsCard>() {
            @Override
            protected void updateItem(FitsCard item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    String name;
                    if (showFiles) {
                        name = cardsInFiles.get(item).getFilename();
                    } else {
                        name = item.getKeywordName();
                    }
                    setText(name + VALUE_SEPARATOR + "'" + item.getValue() + "'");
                }
            }
        });
        listViewCardValues.setPlaceholder(new Label("No cards"));
        listViewCardValues.getItems().addAll(cards);
        listViewCardValues.getSelectionModel().selectedItemProperty().addListener((ov, before, after) -> buttonCardValuesAdd.setDisable(after == null));
        listViewCardValues.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                buttonCardValuesAdd.fire();
                keyEvent.consume();
            }
        });
        listViewConcatenation.getSelectionModel().selectedIndexProperty().addListener((ov, before, after) -> {
            int index = after.intValue();
            buttonCopyValue.setDisable(index == -1);
            buttonRemove.setDisable(index == -1);
            checkMoveButtons(index, listViewConcatenation.getItems().size());
        });
        listViewConcatenation.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText("'" + item + "'");
                }
            }
        });
        listViewConcatenation.setPlaceholder(new Label("No values"));
        textFieldCustomValue.textProperty().addListener((ov, before, after) -> buttonCustomValueAdd.setDisable(after.isEmpty()));
        textFieldCustomValue.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                buttonCustomValueAdd.fire();
                keyEvent.consume();
            }
        });
    }

    /**
     * Handles click on button add card value - adds selected card value to concatenation
     */
    @FXML
    private void handleClickButtonCardValuesAdd() {
        addValueToListViewConcatenation(listViewCardValues.getSelectionModel().getSelectedItem().getValue());
    }

    /**
     * Handles click on button add custom value - adds typed custom value to concatenation
     */
    @FXML
    private void handleClickButtonCustomValueAdd() {
        addValueToListViewConcatenation(textFieldCustomValue.getText());
    }

    /**
     * Handles click on button copy value - copies selected concatenation value
     */
    @FXML
    private void handleClickButtonCopyValue() {
        addValueToListViewConcatenation(listViewConcatenation.getSelectionModel().getSelectedItem());
    }

    /**
     * Handles click on button remove - removes selected concatenation value
     */
    @FXML
    private void handleClickButtonRemove() {
        listViewConcatenation.getItems().remove(listViewConcatenation.getSelectionModel().getSelectedIndex());
        checkMoveButtons(listViewConcatenation.getSelectionModel().getSelectedIndex(), listViewConcatenation.getItems().size());
    }

    /**
     * Handles click on button move up - moves selected concatenation value up
     */
    @FXML
    private void handleClickButtonMoveUp() {
        moveSelectedValue(-1);
        buttonMoveUp.requestFocus();
    }

    /**
     * Handles click on button move down - moves selected concatenation value down
     */
    @FXML
    private void handleClickButtonMoveDown() {
        moveSelectedValue(1);
        buttonMoveDown.requestFocus();
    }

    /**
     * Handles click on button Use concatenation - stores info about dialog result and closes this dialog
     */
    @FXML
    private void handleClickButtonUseConcatenation() {
        useConcatenation = true;
        stage.close();
    }

    /**
     * Handles click on button preview of concatenation - shows preview of concatenation result
     */
    @FXML
    private void handleClickButtonPreviewConcatenation() {
        GUIHelpers.showAlert(AlertType.INFORMATION, "Concatenation preview", "Result of concatenation (without first and last apostrophes):", '\'' + concatenate() + '\'');
    }

    /**
     * Handles click on button cancel - closes this dialog
     */
    @FXML
    private void handleClickButtonCancel() {
        stage.close();
    }

    /**
     * Adds value to concatenation list view
     *
     * @param value value to be added
     */
    private void addValueToListViewConcatenation(String value) {
        int index = listViewConcatenation.getSelectionModel().getSelectedIndex();
        if (index != -1) {
            listViewConcatenation.getItems().add(index, value);
        } else {
            listViewConcatenation.getItems().add(value);
        }
    }

    /**
     * Checks move buttons and set their disable state according indexes
     *
     * @param selectedIndex selectedIndex in list view
     * @param items         count of items in list view
     */
    private void checkMoveButtons(int selectedIndex, int items) {
        GUIHelpers.checkMoveButtons(selectedIndex, items, buttonMoveUp, buttonMoveDown);
    }

    /**
     * Moves selected card in concatenation list view with given offset
     *
     * @param offset how many lines card should be moved
     */
    private void moveSelectedValue(int offset) {
        int selectedIndex = listViewConcatenation.getSelectionModel().getSelectedIndex();
        String movedValue = listViewConcatenation.getItems().set(selectedIndex + offset, listViewConcatenation.getSelectionModel().getSelectedItem());
        listViewConcatenation.getItems().set(selectedIndex, movedValue);
        listViewConcatenation.getSelectionModel().select(selectedIndex + offset);
    }

    /**
     * Returns result of concatenation
     *
     * @return result of concatenation
     */
    private String concatenate() {
        return listViewConcatenation.getItems().stream().collect(Collectors.joining(textFieldGlue.getText()));
    }

    /**
     * Returns result of this dialog
     *
     * @return result of this dialog, if return value is null, then it was not selected option use concatenation; returns pair, first value is index of card target (-1 means new card), second value is result od concatenation
     */
    public Pair<FitsCard, String> getResult() {
        if (!useConcatenation) {
            return null;
        }
        return new Pair<>(choiceBoxTarget.getValue(), concatenate());
    }
}
