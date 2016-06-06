package cz.muni.sci.astro.fhm.gui.controlls;

import cz.muni.sci.astro.fits.FitsCard;
import javafx.event.Event;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;

/**
 * Component for editing FITS card fields
 *
 * @author Jan Hlava, 395986
 */
public class TextFieldFitsCardTableCell extends TextFieldTableCell<FitsCard, String> {
    private final TextField textField;

    /**
     * Creates new instance of this component
     */
    public TextFieldFitsCardTableCell() {
        textField = new TextField();
        textField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                commitEdit(textField.getText());
                requestFocus();
                getTableView().requestFocus();
            } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                textField.setText(getItem());
                cancelEdit();
            }
        });
        textField.focusedProperty().addListener((ov, before, after) -> {
            if (!after) {
                commitEdit(textField.getText());
            }
        });
    }

    /**
     * Handles start of editing - sets text
     */
    @Override
    public void startEdit() {
        super.startEdit();
        textField.setText(getItem());
        setText(null);
        setGraphic(textField);
        textField.selectAll();
        textField.requestFocus();
    }

    /**
     * Handles updating item
     *
     * @param item  item to update
     * @param empty is item empty
     */
    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                setText(null);
                setGraphic(textField);
            } else {
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
    public void commitEdit(String item) {
        if (!isEditing() && (item == null || !item.equals(getItem()))) {
            TableView<FitsCard> table = getTableView();
            if (table != null) {
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
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem());
        setGraphic(null);
    }
}
