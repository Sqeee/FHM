package cz.muni.sci.astro.fhm.gui;

import cz.muni.sci.astro.fhm.core.Helpers;
import cz.muni.sci.astro.fits.FitsCardDateValue;
import cz.muni.sci.astro.fits.FitsCardDateValueUnknownFormatException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Controller for converter
 *
 * @author Jan Hlava, 395986
 */
public class ConverterController
{
    public static final String ALERT_TITLE = "Conversion error";

    @FXML private TextField textFieldUTC;
    @FXML private TextField textFieldJD;
    @FXML private TextField textFieldResult;

    /**
     * Handles click on button for conversion from UTC to JD and do the conversion
     */
    @FXML private void handleClickButtonUTC_To_JD()
    {
        try
        {
            FitsCardDateValue dateUTC = FitsCardDateValue.createFromDateString(textFieldUTC.getText());
            textFieldResult.setText(Helpers.formatDouble(dateUTC.getJulianDay()));
        }
        catch (FitsCardDateValueUnknownFormatException exc)
        {
            GUIHelpers.showAlert(AlertType.ERROR, ALERT_TITLE, "UTC to JD conversion failed.", "Unknown format of UTC date.");
        }
    }

    /**
     * Handles click on button for conversion from JD to UTC and do the conversion
     */
    @FXML private void handleClickButtonJD_To_UTC()
    {
        try
        {
            FitsCardDateValue dateJD = FitsCardDateValue.parseJDString(textFieldJD.getText());
            textFieldResult.setText(dateJD.toString());
        }
        catch (NumberFormatException exc)
        {
            GUIHelpers.showAlert(AlertType.ERROR, ALERT_TITLE, "JD to UTC conversion failed.", "JD should be non negative number");
        }
    }

    /**
     * Handles key pressed event on text fields and count pressing enter as click on according button
     *
     * @param event key event of key pressed
     */
    public void handleKeyPressedOnTextField(KeyEvent event)
    {
        if (event.getCode() == KeyCode.ENTER)
        {
            if (event.getSource().equals(textFieldUTC))
            {
                handleClickButtonUTC_To_JD();
            }
            else if (event.getSource().equals(textFieldJD))
            {
                handleClickButtonJD_To_UTC();
            }
        }
    }
}
