package cz.muni.sci.astro.fhm.gui.controlls;

import cz.muni.sci.astro.fits.FitsCard;
import cz.muni.sci.astro.fits.FitsFile;
import javafx.event.Event;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;

/**
 * Component for editing FITS card keyword
 *
 * @author Jan Hlava, 395986
 */
public class TextFieldKeywordTableCell extends TableCell<FitsCard, String>
{
    private final TextFieldMaxLength textFieldKeyword;

    /**
     * Creates new instance of this component
     */
    public TextFieldKeywordTableCell()
    {
        textFieldKeyword = new TextFieldMaxLength("", FitsFile.KEYWORD_LENGTH);
        textFieldKeyword.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER)
            {
                commitEdit(textFieldKeyword.getText());
                requestFocus();
                getTableView().requestFocus();
            }
            else if (keyEvent.getCode() == KeyCode.ESCAPE)
            {
                textFieldKeyword.setText(getItem());
                cancelEdit();
            }
        });
        textFieldKeyword.focusedProperty().addListener((ov, before, after) -> {
            if (!after)
            {
                commitEdit(textFieldKeyword.getText());
            }
        });
    }

    /**
     * Handles start of editing - sets text
     */
    @Override
    public void startEdit()
    {
        super.startEdit();
        textFieldKeyword.setText(getItem());
        setText(null);
        setGraphic(textFieldKeyword);
        textFieldKeyword.selectAll();
        textFieldKeyword.requestFocus();
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
                setGraphic(textFieldKeyword);
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
