package cz.muni.sci.astro.fhm.gui.controlls;

import cz.muni.sci.astro.fits.FitsCard;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.input.KeyCode;

import java.util.List;

/**
 * Component for editing file, where FITS card is stored
 *
 * @author Jan Hlava, 395986
 */
public class ComboBoxFitsFileTableCell extends TableCell<FitsCard, String>
{
    private final ComboBox<String> comboBox;
    private static List<String> files;

    /**
     * Creates new instance of this component
     */
    public ComboBoxFitsFileTableCell()
    {
        comboBox = new ComboBox<>();
        comboBox.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER)
            {
                commitEdit(comboBox.getValue());
                requestFocus();
                getTableView().requestFocus();
            }
            else if (keyEvent.getCode() == KeyCode.ESCAPE)
            {
                comboBox.setValue(getItem());
                cancelEdit();
            }
        });
        comboBox.focusedProperty().addListener((ov, before, after) -> {
            if (!after)
            {
                commitEdit(comboBox.getValue());
            }
        });
    }

    /**
     * Set files for combo box
     *
     * @param filesList list of files for combo box
     */
    public static void setFiles(List<String> filesList)
    {
        files = filesList;
    }

    /**
     * Handles start of editing
     */
    @Override
    public void startEdit()
    {
        if (!isEmpty())
        {
            super.startEdit();
            comboBox.setItems(FXCollections.observableArrayList(files));
            comboBox.getSelectionModel().select(getItem());
            comboBox.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue)
                {
                    commitEdit(comboBox.getSelectionModel().getSelectedItem());
                }
            });
            setText(null);
            setGraphic(comboBox);
        }
    }

    /**
     * Handles updating item
     *
     * @param item item to update
     * @param empty is item empty
     */
    @Override
    public void updateItem(String item, boolean empty)
    {
        super.updateItem(item, empty);
        if (empty)
        {
            setText(null);
            setGraphic(null);
        }
        else
        {
            if (isEditing())
            {
                setText(null);
                setGraphic(comboBox);
            }
            else
            {
                setText(getItem());
                setGraphic(null);
            }
        }
    }

    /**
     * Handles committing edit
     *
     * @param item item to commit
     */
    @Override
    public void commitEdit(String item)
    {
        if (!isEditing() && (item == null || !item.equals(getItem())))
        {
            TableView<FitsCard> table = getTableView();
            if (table != null)
            {
                TableColumn<FitsCard, String> column = getTableColumn();
                CellEditEvent<FitsCard, String> event = new CellEditEvent<>(table, new TablePosition<>(table, getIndex(), column), TableColumn.editCommitEvent(), item);
                Event.fireEvent(column, event);
            }
        }
        super.commitEdit(item);
    }

    /**
     * Handles cancel of editing
     */
    @Override
    public void cancelEdit()
    {
        super.cancelEdit();
        setText(getItem());
        setGraphic(null);
    }
}
