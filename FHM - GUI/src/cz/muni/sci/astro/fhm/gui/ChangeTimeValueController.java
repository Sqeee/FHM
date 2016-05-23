package cz.muni.sci.astro.fhm.gui;

import cz.muni.sci.astro.fhm.core.Helpers;
import cz.muni.sci.astro.fits.FitsCard;
import cz.muni.sci.astro.fits.FitsCardDateValue;
import cz.muni.sci.astro.fits.FitsCardDateValueUnknownFormatException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for changing time values of cards
 *
 * @author Jan Hlava, 395986
 */
public class ChangeTimeValueController
{
    private static final String ALERT_TITLE = "Change time value error";
    private static final String FORMAT_SEPARATOR = " -> ";

    private FitsCard card;
    private Map<String, ChronoUnit> dateTimeOptions;
    private FitsCardDateValue actualDateTime;
    private Stage stage;
    private boolean changedTextFieldActualValue;

    @FXML private TextField textFieldOriginalValue;
    @FXML private TextField textFieldActualValue;
    @FXML private TextField textFieldShifting;
    @FXML private ChoiceBox<String> choiceBoxShifting;
    @FXML private ChoiceBox<String> choiceBoxUTC_Format;
    @FXML private Button buttonChangeUTC_Format;
    @FXML private Toggle toggleUTC_Date;
    @FXML private Toggle toggleJD_Date;
    @FXML private ToggleGroup toggleGroupDateMode;

    /**
     * Prepares this dialog
     *
     * @param card card which date time value we want to modify
     * @param stage stage of window
     */
    public void prepareWindow(FitsCard card, Stage stage)
    {
        this.card = card;
        this.stage = stage;
        changedTextFieldActualValue = true;
        textFieldActualValue.setOnKeyPressed(keyEvent -> changedTextFieldActualValue = true);
        textFieldShifting.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER)
            {
                handleClickButtonShift();
                keyEvent.consume();
            }
        });
        choiceBoxShifting.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER)
            {
                handleClickButtonShift();
                keyEvent.consume();
            }
            else if (keyEvent.getCode() == KeyCode.ESCAPE)
            {
                choiceBoxShifting.hide();
                keyEvent.consume();
            }
        });
        choiceBoxUTC_Format.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER)
            {
                handleClickButtonChangeUTC_Format();
                keyEvent.consume();
            }
            else if (keyEvent.getCode() == KeyCode.ESCAPE)
            {
                choiceBoxShifting.hide();
                keyEvent.consume();
            }
        });
        textFieldOriginalValue.setText(card.getRValueString());
        if (!card.getDateValue().isCreatedFromClassicDate())
        {
            changeDateMode(toggleJD_Date);
        }
        changeActualValue(textFieldOriginalValue.getText());
        dateTimeOptions = new HashMap<>();
        dateTimeOptions.put("Microseconds", ChronoUnit.MICROS);
        dateTimeOptions.put("Milliseconds", ChronoUnit.MILLIS);
        dateTimeOptions.put("Seconds", ChronoUnit.SECONDS);
        dateTimeOptions.put("Minutes", ChronoUnit.MINUTES);
        dateTimeOptions.put("Hours", ChronoUnit.HOURS);
        dateTimeOptions.put("Days", ChronoUnit.DAYS);
        dateTimeOptions.put("Months", ChronoUnit.MONTHS);
        dateTimeOptions.put("Years", ChronoUnit.YEARS);
        choiceBoxShifting.setItems(FXCollections.observableArrayList(dateTimeOptions.keySet()));
        choiceBoxShifting.getItems().sort((u1, u2) -> dateTimeOptions.get(u1).compareTo(dateTimeOptions.get(u2)));
        try
        {
            FitsCardDateValue now = new FitsCardDateValue();
            for (String format : FitsCardDateValue.getAcceptableFormats())
            {
                now.setFormat(format);
                choiceBoxUTC_Format.getItems().add(format + FORMAT_SEPARATOR + now.toString());
            }
        }
        catch (FitsCardDateValueUnknownFormatException ignored)// Should not be raised, because I iterate through acceptable formats
        {}
        choiceBoxUTC_Format.getSelectionModel().select(FitsCardDateValue.getAcceptableFormats().indexOf(actualDateTime.getFormat()));
    }

    /**
     * Handles click on toggles of date mode - change view of actual value to new date mode if it is valid date
     */
    @FXML private void handleClickToggleDateMode()
    {
        Toggle before;
        if (toggleGroupDateMode.getSelectedToggle() == toggleJD_Date)
        {
            before = toggleUTC_Date;
        }
        else
        {
            before = toggleJD_Date;
        }
        if (checkActualDateTime(before))
        {
            updateTextFieldActualValue();
            formattingSetDisable(toggleGroupDateMode.getSelectedToggle() == toggleJD_Date);
        }
        else
        {
            toggleGroupDateMode.selectToggle(before);
        }
    }

    /**
     * Handles click on Shift button - do the shift of actual date time value
     */
    @FXML private void handleClickButtonShift()
    {
        try
        {
            int shift = Integer.parseInt(textFieldShifting.getText());
            if (choiceBoxShifting.getSelectionModel().getSelectedIndex() == -1)
            {
                GUIHelpers.showAlert(AlertType.ERROR, ALERT_TITLE, "Shifting value failed.", "Time interval was not selected.");
                return;
            }
            if (checkActualDateTime())
            {
                actualDateTime.doShift(shift, dateTimeOptions.get(choiceBoxShifting.getSelectionModel().getSelectedItem()));
                updateTextFieldActualValue();
            }
        }
        catch (NumberFormatException exc)
        {
            GUIHelpers.showAlert(AlertType.ERROR, ALERT_TITLE, "Shifting value failed.", "Shift value must be integer.");
        }
    }

    /**
     * Handles click on button use actual value - checks actual value and if it is OK, change card record and close this dialog
     */
    @FXML private void handleClickButtonUseActualValue()
    {
        changedTextFieldActualValue = true; // Force to check actual value, not cached version of actual value
        if (checkActualDateTime())
        {
            if (card.isJulianDayDateValue() && card.getRValueString().contains(".") && !textFieldActualValue.getText().contains(".")) // If original value was JD date in double format and new value is in integer format adds ".0" at the end of value - reason for this behaviour is that I do not want to change data type
            {
                textFieldActualValue.setText(textFieldActualValue.getText() + ".0");
            }
            card.setRValue(textFieldActualValue.getText());
            stage.close();
        }
    }

    /**
     * Handles click on button use default value - close this dialog
     */
    @FXML private void handleClickButtonUseDefaultValue()
    {
        stage.close();
    }

    /**
     * Handles click on button copy to actual - copies original value to actual value and restore to original mode
     */
    @FXML private void handleClickButtonCopyToActual()
    {
        if (card.getDateValue().isCreatedFromClassicDate())
        {
            changeDateMode(toggleUTC_Date);
        }
        else
        {
            changeDateMode(toggleJD_Date);
        }
        changeActualValue(textFieldOriginalValue.getText());
    }

    /**
     * Handles click on button change UTC format - change displayed format of UTC format
     */
    @FXML private void handleClickButtonChangeUTC_Format()
    {
        if (choiceBoxUTC_Format.getSelectionModel().getSelectedIndex() == -1)
        {
            GUIHelpers.showAlert(AlertType.ERROR, ALERT_TITLE, "Change of UTC format failed.", "You must select one of listed formats");
        }
        if (checkActualDateTime())
        {
            updateTextFieldActualValue();
        }
    }

    /**
     * Checks if actual value of date time is valid with selected date mode
     *
     * @return true if actual value of date time is valid with selected date mode, otherwise false
     */
    private boolean checkActualDateTime()
    {
        return checkActualDateTime(toggleGroupDateMode.getSelectedToggle());
    }

    /**
     * Checks if actual value of date time is valid with given date mode
     *
     * @return true if actual value of date time is valid with given date mode, otherwise false
     */
    private boolean checkActualDateTime(Toggle dateMode)
    {
        if (!changedTextFieldActualValue) // In case changing modes -> it could be changing actual value -> UTC to JD and then UTC could produce different date
        {
            return true;
        }
        if (dateMode == toggleUTC_Date)
        {
            return checkActualDateTimeUTC();
        }
        else
        {
            return checkActualDateTimeJD();
        }
    }

    /**
     * Checks if actual value of date time is valid UTC date
     *
     * @return true if actual value of date time is valid UTC date, otherwise false
     */
    private boolean checkActualDateTimeUTC()
    {
        try
        {
            actualDateTime = FitsCardDateValue.createFromDateString(textFieldActualValue.getText());
            changedTextFieldActualValue = false;
            return true;
        }
        catch (FitsCardDateValueUnknownFormatException exc)
        {
            GUIHelpers.showAlert(AlertType.ERROR, ALERT_TITLE, "Actual date time value error.", "Actual date has unknown format of UTC date.");
            return false;
        }
    }

    /**
     * Checks if actual value of date time is valid JD date
     *
     * @return true if actual value of date time is valid JD date, otherwise false
     */
    private boolean checkActualDateTimeJD()
    {
        try
        {
            actualDateTime = FitsCardDateValue.parseJDString(textFieldActualValue.getText());
            changedTextFieldActualValue = false;
            return true;
        }
        catch (NumberFormatException exc)
        {
            GUIHelpers.showAlert(AlertType.ERROR, ALERT_TITLE, "Actual date time value error.", "Actual date has invalid format of Julian Day - it must be non negative number.");
            return false;
        }
    }

    /**
     * Update text in text field of actual value
     */
    private void updateTextFieldActualValue()
    {
        if (toggleGroupDateMode.getSelectedToggle() == toggleUTC_Date)
        {
            updateFormat();
            textFieldActualValue.setText(actualDateTime.toString());
        }
        else
        {
            textFieldActualValue.setText(Helpers.formatDouble(actualDateTime.getJulianDay()));
        }
    }

    /**
     * Change actual value (and also checks it and updates format)
     *
     * @param newValue new value of actual value
     */
    private void changeActualValue(String newValue)
    {
        textFieldActualValue.setText(newValue);
        changedTextFieldActualValue = true;
        checkActualDateTime();
        updateFormat();
    }

    /**
     * Changes date mode
     *
     * @param toggle new date mode
     */
    private void changeDateMode(Toggle toggle)
    {
        toggleGroupDateMode.selectToggle(toggle);
        formattingSetDisable(toggle == toggleJD_Date);
    }

    /**
     * Set disable state of formatting inputs
     *
     * @param disable new disable state
     */
    private void formattingSetDisable(boolean disable)
    {
        choiceBoxUTC_Format.setDisable(disable);
        buttonChangeUTC_Format.setDisable(disable);
    }

    /**
     * Updates format of given date time value
     */
    private void updateFormat()
    {
        if (choiceBoxUTC_Format.getValue() == null)
        {
            return;
        }
        try
        {
            int endIndex = choiceBoxUTC_Format.getValue().indexOf(FORMAT_SEPARATOR);
            actualDateTime.setFormat(choiceBoxUTC_Format.getValue().substring(0, endIndex));
        }
        catch (FitsCardDateValueUnknownFormatException ignored) // Should not occurred, choice box is filled with acceptable formats
        {}
    }
}
