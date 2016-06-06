package cz.muni.sci.astro.fhm.gui;

import cz.muni.sci.astro.fhm.gui.controlls.ComboBoxFitsFileTableCell;
import cz.muni.sci.astro.fhm.gui.controlls.TextFieldFitsCardTableCell;
import cz.muni.sci.astro.fhm.gui.controlls.TextFieldKeywordTableCell;
import cz.muni.sci.astro.fits.FitsCard;
import cz.muni.sci.astro.fits.FitsException;
import cz.muni.sci.astro.fits.FitsFile;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for editing cards
 *
 * @author Jan Hlava, 395986
 */
public class EditController {
    private static final String EDIT_TAB = "fxml/EditTab.fxml";
    private static final String BLANK_CARD_TAB_NAME = "(BLANK)";
    private static final Label LABEL_NO_CARDS = new Label("No cards");
    private static final Comparator<Tab> TAB_ALPHABET_COMPARATOR = (tab1, tab2) -> tab1.getText().compareTo(tab2.getText());
    private static final int SUBJECT_COLUMN = 0;
    private static final int DATATYPE_COLUMN = 1;
    private static final int RVALUE_COLUMN = 2;
    private static final int IVALUE_COLUMN = 3;
    private static final int COMMENT_COLUMN = 4;
    private MainViewController mainViewController;
    private List<String> files;
    private List<FitsFile> fitsFiles;
    private Map<FitsCard, FitsFile> cardsInFiles;
    private Map<String, List<FitsCard>> cardsList;
    private List<Tab> tabsFiles;
    private List<Tab> tabsCards;
    private List<Tab> tabsCardsToRemove;
    @FXML
    private TabPane tabPaneDatas;
    @FXML
    private ToggleGroup toggleGroupMode;
    @FXML
    private Toggle toggleModeFiles;
    @FXML
    private Toggle toggleModeKeywords;
    @FXML
    private Button buttonAddNew;
    @FXML
    private Button buttonDeleteCard;
    @FXML
    private Button buttonMoveUp;
    @FXML
    private Button buttonMoveDown;
    @FXML
    private Button buttonSave;
    @FXML
    private Button buttonSaveQuit;

    /**
     * Prepares this form - load tabs into tabView
     *
     * @param filenames          file list to working with
     * @param mainViewController MainViewController controller
     */
    public void prepareWindow(List<String> filenames, MainViewController mainViewController) {
        this.mainViewController = mainViewController;
        GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_FILE, MenuBarController.MENU_ITEM_FILE_SAVE, false, e -> handleClickButtonSave());
        GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_FILE, MenuBarController.MENU_ITEM_FILE_EXIT, false, e -> quitWithConfirmation());
        GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_TOOLS, MenuBarController.MENU_ITEM_TOOLS_CONCATENATION_MANAGER, false, e -> openConcatenationManagerDialog());
        GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_MODES, MenuBarController.MENU_ITEM_MODES_SINGLE, true, null);
        GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_MODES, MenuBarController.MENU_ITEM_MODES_MULTIPLE, false, e -> switchMode());
        files = filenames;
        cardsInFiles = new HashMap<>();
        cardsList = new HashMap<>();
        List<String> errors = new ArrayList<>();
        tabsCards = new ArrayList<>();
        tabsCardsToRemove = new ArrayList<>();
        tabsFiles = new ArrayList<>();
        fitsFiles = new ArrayList<>();
        for (String filename : files) {
            File file = new File(filename);
            try {
                FitsFile fitsFile = new FitsFile(file);
                fitsFiles.add(fitsFile);
                tabsFiles.add(newEditTab());
            } catch (FitsException | IOException exc) {
                errors.add("Error: cannot load file " + file.getName() + " with reason " + exc.getMessage());
            }
        }
        ComboBoxFitsFileTableCell.setFiles(files);
        prepareTabPane();
        if (tabPaneDatas.getTabs().isEmpty()) {
            buttonAddNew.setDisable(true);
            buttonSave.setDisable(true);
            buttonSaveQuit.setDisable(true);
            GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_FILE, MenuBarController.MENU_ITEM_FILE_SAVE, true, null);
            GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_TOOLS, MenuBarController.MENU_ITEM_TOOLS_CONCATENATION_MANAGER, true, null);
            ((RadioButton) toggleModeFiles).setDisable(true);
            ((RadioButton) toggleModeKeywords).setDisable(true);
        }
        if (!errors.isEmpty()) {
            GUIHelpers.showAlert(AlertType.ERROR, "Error", "Error occurred", String.join("\n", errors));
        }
    }

    /**
     * Prepares tabs with TableView and fills them with data
     */
    private void prepareTabPane() {
        Tab tab;
        for (int i = 0; i < tabsFiles.size(); i++) {
            prepareFileTab(tabsFiles.get(i), fitsFiles.get(i));
        }
        for (String cardKeyword : cardsList.keySet()) {
            try {
                tab = newEditTab();
            } catch (IOException ignored) // It should not occur - proper creating of new instance
            {
                continue;
            }
            tabsCards.add(tab);
            prepareCardTab(tab, cardKeyword);
        }
        tabsCards.sort(TAB_ALPHABET_COMPARATOR);
        tabPaneDatas.getTabs().addAll(tabsFiles);
        tabPaneDatas.getSelectionModel().selectedItemProperty().addListener((ov, before, after) -> {
            if (after == null) {
                setDisableDeleteButtons(true);
                buttonMoveDown.setDisable(true);
                buttonMoveUp.setDisable(true);
            } else {
                TableView<FitsCard> tableView = getTableViewInTab(after);
                FitsCard card = getSelectedCardInTableView(tableView);
                setDisableDeleteButtons(card == null);
                checkMoveButtons(getSelectedIndexInTableView(tableView), tableView.getItems().size());
            }
        });
    }

    /**
     * Prepares file tab
     *
     * @param tab  tab to be prepared
     * @param file file for association with given tab
     */
    private void prepareFileTab(Tab tab, FitsFile file) {
        tab.setText(file.getFilename());
        TableView<FitsCard> tableView = getTableViewInTab(tab);
        EventHandler<CellEditEvent<FitsCard, String>> subjectSetOnEditCommit = t -> {
            if (t.getOldValue().equals(t.getNewValue())) {
                return;
            }
            Tab tab1 = getTabWithName(tabsCards, t.getOldValue());
            t.getRowValue().setKeyword(t.getNewValue());
            String keyword = t.getRowValue().getKeywordName();
            TableView<FitsCard> tabTableView = getTableViewInTab(tab1);
            tabTableView.getItems().remove(t.getRowValue());
            if (tabTableView.getItems().isEmpty()) {
                tabsCards.remove(tab1);
            }
            tab1 = getTabWithName(tabsCards, keyword);
            if (tab1 == null) {
                try {
                    tab1 = newEditTab();
                } catch (IOException ignored) // It should not occur - proper creating of new instance
                {
                }
                tabsCards.add(tab1);
                cardsList.put(keyword, new ArrayList<>());
                prepareCardTab(tab1, keyword);
                tabsCards.sort(TAB_ALPHABET_COMPARATOR);
            }
            tabTableView = getTableViewInTab(tab1);
            tabTableView.getItems().add(t.getRowValue());
            refreshContentTableView(t.getTableColumn());
        };
        prepareTabView(tableView, false, "Keyword", cellCard -> new SimpleStringProperty(cellCard.getValue().getKeywordName()), tableColumn -> new TextFieldKeywordTableCell(), subjectSetOnEditCommit);
        List<FitsCard> cards = file.getHDU(0).getHeader().getCards(); // Read only primary header, need change in case of need read other headers, change needs also method handleClickButtonSave
        tableView.setItems(FXCollections.observableList(cards));
        String cardKeyword;
        for (FitsCard card : cards) {
            cardsInFiles.put(card, file);
            cardKeyword = card.getKeywordName();
            if (cardKeyword.trim().isEmpty()) {
                cardKeyword = BLANK_CARD_TAB_NAME;
            }
            if (!cardsList.containsKey(cardKeyword)) {
                cardsList.put(cardKeyword, new ArrayList<>());
            }
            cardsList.get(cardKeyword).add(card);
        }
    }

    /**
     * Prepares card tab
     *
     * @param tab      tab to be prepared
     * @param cardName card name for association with given tab
     */
    private void prepareCardTab(Tab tab, String cardName) {
        if (cardName.trim().isEmpty()) {
            cardName = BLANK_CARD_TAB_NAME;
        }
        tab.setText(cardName);
        TableView<FitsCard> tableView = getTableViewInTab(tab);
        EventHandler<CellEditEvent<FitsCard, String>> subjectSetOnEditCommit = t -> {
            if (t.getOldValue().equals(t.getNewValue())) {
                return;
            }
            TableView<FitsCard> tabTableView = getTableViewInTab(getTabWithName(tabsFiles, t.getNewValue()));
            tabTableView.getItems().add(tabTableView.getItems().size() - 1, t.getRowValue());
            tabTableView = getTableViewInTab(getTabWithName(tabsFiles, t.getOldValue()));
            tabTableView.getItems().remove(t.getRowValue());
            fitsFiles.stream().filter(fitsFile -> fitsFile.getFilename().equals(t.getNewValue())).forEach(fitsFile -> cardsInFiles.put(t.getRowValue(), fitsFile));
            refreshContentTableView(t.getTableColumn());
        };
        prepareTabView(tableView, true, "Filename", cellCard -> new SimpleStringProperty(cardsInFiles.get(cellCard.getValue()).getFilename()), tableColumn -> new ComboBoxFitsFileTableCell(), subjectSetOnEditCommit);
        List<FitsCard> cards = cardsList.get(cardName);
        tableView.setItems(FXCollections.observableList(cards));
    }

    /**
     * Prepares table view
     *
     * @param tableView               what should be prepared
     * @param subject                 name of subject column
     * @param subjectCellFactory      cell factory for subject column
     * @param subjectCellValueFactory cell value factory for subject column
     * @param subjectSetOnEditCommit  action on edit commit on subject column
     */
    @SuppressWarnings("unchecked")
    private void prepareTabView(TableView<FitsCard> tableView, boolean sortableColumns, String subject, Callback<CellDataFeatures<FitsCard, String>, ObservableValue<String>> subjectCellValueFactory, Callback<TableColumn<FitsCard, String>, TableCell<FitsCard, String>> subjectCellFactory, EventHandler<CellEditEvent<FitsCard, String>> subjectSetOnEditCommit) {
        tableView.setPlaceholder(LABEL_NO_CARDS);
        tableView.getSelectionModel().selectedIndexProperty().addListener((ov, before, after) -> {
            if (after.intValue() == -1) {
                setDisableDeleteButtons(true);
                buttonMoveDown.setDisable(true);
                buttonMoveUp.setDisable(true);
            } else {
                if (before.intValue() == -1) {
                    setDisableDeleteButtons(false);
                }
                if (getSelectedTableView() == tableView) {
                    checkMoveButtons(after.intValue(), tableView.getItems().size());
                }
            }
        });
        TableColumn<FitsCard, String> tableColumnSubject = (TableColumn<FitsCard, String>) tableView.getColumns().get(SUBJECT_COLUMN);
        TableColumn<FitsCard, String> tableColumnRValue = (TableColumn<FitsCard, String>) tableView.getColumns().get(RVALUE_COLUMN);
        TableColumn<FitsCard, String> tableColumnIValue = (TableColumn<FitsCard, String>) tableView.getColumns().get(IVALUE_COLUMN);
        TableColumn<FitsCard, String> tableColumnDataType = (TableColumn<FitsCard, String>) tableView.getColumns().get(DATATYPE_COLUMN);
        TableColumn<FitsCard, String> tableColumnComment = (TableColumn<FitsCard, String>) tableView.getColumns().get(COMMENT_COLUMN);
        tableColumnSubject.setSortable(sortableColumns);
        tableColumnRValue.setSortable(sortableColumns);
        tableColumnIValue.setSortable(sortableColumns);
        tableColumnDataType.setSortable(sortableColumns);
        tableColumnComment.setSortable(sortableColumns);
        tableColumnSubject.setStyle("-fx-alignment: CENTER-LEFT;");
        tableColumnRValue.setStyle("-fx-alignment: CENTER-LEFT;");
        tableColumnIValue.setStyle("-fx-alignment: CENTER-LEFT;");
        tableColumnDataType.setStyle("-fx-alignment: CENTER-LEFT;");
        tableColumnComment.setStyle("-fx-alignment: CENTER-LEFT;");
        tableColumnSubject.setText(subject);
        tableColumnSubject.setCellValueFactory(subjectCellValueFactory);
        tableColumnRValue.setCellValueFactory(cellCard -> new SimpleStringProperty(cellCard.getValue().getRValueString()));
        tableColumnIValue.setCellValueFactory(cellCard -> new SimpleStringProperty(cellCard.getValue().getIValueString()));
        tableColumnDataType.setCellValueFactory(cellCard -> new SimpleStringProperty(cellCard.getValue().getKeyword().getType().toString()));
        tableColumnComment.setCellValueFactory(cellCard -> new SimpleStringProperty(cellCard.getValue().getComment()));
        tableColumnSubject.setCellFactory(subjectCellFactory);
        Callback<TableColumn<FitsCard, String>, TableCell<FitsCard, String>> cellTextFieldFitsCardFactory = tableColumn -> new TextFieldFitsCardTableCell();
        tableColumnRValue.setCellFactory(cellTextFieldFitsCardFactory);
        tableColumnIValue.setCellFactory(cellTextFieldFitsCardFactory);
        tableColumnComment.setCellFactory(cellTextFieldFitsCardFactory);
        tableColumnSubject.setOnEditCommit(subjectSetOnEditCommit);
        tableColumnRValue.setOnEditCommit(t -> {
            t.getRowValue().setRValue(t.getNewValue());
            refreshContentTableView(t.getTableView().getColumns().get(RVALUE_COLUMN));
        });
        tableColumnIValue.setOnEditCommit(t -> {
            t.getRowValue().setIValue(t.getNewValue());
            refreshContentTableView(t.getTableView().getColumns().get(IVALUE_COLUMN));
        });
        tableColumnComment.setOnEditCommit(t -> {
            t.getRowValue().setComment(t.getNewValue());
            refreshContentTableView(t.getTableView().getColumns().get(COMMENT_COLUMN));
        });
        tableView.setRowFactory(fitsCardTableView -> {
            TableRow<FitsCard> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem addNewMenuItem = new MenuItem("Add new Card");
            addNewMenuItem.setOnAction(e -> handleClickButtonNewCard());
            MenuItem deleteMenuItem = new MenuItem("Delete Card");
            deleteMenuItem.setOnAction(e -> handleClickButtonDeleteCard());
            MenuItem moveUpMenuItem = new MenuItem("Move up");
            moveUpMenuItem.setOnAction(e -> handleClickButtonMoveUp());
            MenuItem moveDownMenuItem = new MenuItem("Move down");
            moveDownMenuItem.setOnAction(e -> handleClickButtonMoveDown());
            MenuItem changeDateTimeValueMenuItem = new MenuItem("Change datetime value...");
            changeDateTimeValueMenuItem.setOnAction(e -> openChangeDateTimeValueDialog());
            contextMenu.getItems().addAll(addNewMenuItem, deleteMenuItem, moveUpMenuItem, moveDownMenuItem, changeDateTimeValueMenuItem);
            contextMenu.setOnShowing(e -> {
                deleteMenuItem.setDisable(buttonDeleteCard.isDisable());
                moveUpMenuItem.setDisable(buttonMoveUp.isDisable());
                moveDownMenuItem.setDisable(buttonMoveDown.isDisable());
                changeDateTimeValueMenuItem.setDisable(!row.getItem().isDateValue());
            });
            row.contextMenuProperty().bind(Bindings.when(Bindings.isNotNull(row.itemProperty())).then(contextMenu).otherwise((ContextMenu) null));
            return row;
        });
        tableView.setFixedCellSize(30.0);
    }

    /**
     * Handles click on button Save - tries to save content, if there are some problems, shows them and return false
     *
     * @return true if save does not failed, otherwise false
     */
    @FXML
    private boolean handleClickButtonSave() {
        List<String> problems;
        for (FitsFile file : fitsFiles) {
            file.getHDU(0).getHeader().setCards(getTableViewInTab(getTabWithName(tabsFiles, file.getFilename())).getItems());
            problems = file.getHDU(0).getHeader().checkSaveHeader();
            if (!problems.isEmpty()) {
                GUIHelpers.showAlert(AlertType.ERROR, "Saving cards", "Nothing was saved. Saving file " + file.getFilename() + " fails with error: ", String.join("\n", problems));
                return false;
            }
        }
        problems = new ArrayList<>();
        problems.addAll(fitsFiles.stream().filter(file -> !file.saveFile()).map(FitsFile::getFilename).collect(Collectors.toList()));
        if (!problems.isEmpty()) {
            GUIHelpers.showAlert(AlertType.ERROR, "Saving cards", "Saving these files failed: ", String.join(", ", problems));
            return false;
        }
        GUIHelpers.showAlert(AlertType.INFORMATION, "Saving cards", "Files were saved successfully.", null);
        return true;
    }

    /**
     * Handles click on button New Card - adds new card to active tab and returns new card
     *
     * @return new card
     */
    @FXML
    private FitsCard handleClickButtonNewCard() {
        TableView<FitsCard> tableView = getSelectedTableView();
        int selectedIndex = getSelectedIndexInTableView(tableView);
        FitsCard newCard;
        Tab newTab;
        if (selectedIndex == -1) {
            selectedIndex = Math.max(tableView.getItems().size() - 1, 0);
        }
        try {
            newCard = new FitsCard();
            tableView.getItems().add(selectedIndex, newCard);
            if (toggleGroupMode.getSelectedToggle() == toggleModeFiles) {
                cardsInFiles.put(newCard, fitsFiles.get(tabPaneDatas.getSelectionModel().getSelectedIndex()));
                if (cardsList.containsKey(BLANK_CARD_TAB_NAME)) {
                    getTableViewInTab(getTabWithName(tabsCards, BLANK_CARD_TAB_NAME)).getItems().add(newCard);
                } else {
                    cardsList.put(BLANK_CARD_TAB_NAME, new ArrayList<>());
                    newTab = newEditTab();
                    tabsCards.add(newTab);
                    cardsList.get(BLANK_CARD_TAB_NAME).add(newCard);
                    prepareCardTab(newTab, BLANK_CARD_TAB_NAME);
                    tabsCards.sort(TAB_ALPHABET_COMPARATOR);
                }
            } else {
                String keyword = getSelectedTab(tabPaneDatas).getText();
                if (!keyword.equals(BLANK_CARD_TAB_NAME)) {
                    newCard.setKeyword(keyword);
                } else {
                    newCard.setKeyword("");
                }
                ObservableList<FitsCard> fileCards = getTableViewInTab(getTabWithName(tabsFiles, fitsFiles.get(0).getFilename())).getItems();
                fileCards.add(fileCards.size() - 1, newCard);
                cardsInFiles.put(newCard, fitsFiles.get(0));
            }
            tableView.scrollTo(newCard);
            return newCard;
        } catch (IOException ignored) // Creating new edit tab should not fail, so ignore this exception
        {
            return null;
        }
    }

    /**
     * Handles click on delete card - deletes selected card
     */
    @FXML
    private void handleClickButtonDeleteCard() {
        TableView<FitsCard> tableView = getSelectedTableView();
        int selectedIndex = getSelectedIndexInTableView(tableView);
        if (selectedIndex == -1) {
            return;
        }
        FitsCard removedCard = getSelectedCardInTableView(tableView);
        tableView.getItems().remove(removedCard);
        Tab tab;
        if (toggleGroupMode.getSelectedToggle() == toggleModeFiles) {
            tab = getTabWithName(tabsCards, removedCard.getKeywordName());
            tableView = getTableViewInTab(tab);
            tableView.getItems().remove(removedCard);
            if (tableView.getItems().isEmpty()) {
                tabsCards.remove(tab);
            }
        } else {
            if (tableView.getItems().isEmpty()) {
                tabsCardsToRemove.add(getSelectedTab(tabPaneDatas));
            }
            tab = getTabWithName(tabsFiles, cardsInFiles.get(removedCard).getFilename());
            tableView = getTableViewInTab(tab);
            tableView.getItems().remove(removedCard);
        }
        if (selectedIndex == 0 && getSelectedTableView().getItems().size() == 1) // if i had selected first row and I deleted, then index was not change, so it is not affected by checkMoveButtons
        {
            buttonMoveDown.setDisable(true);
        }
    }

    /**
     * Handles click on button move up and moves selected card up
     */
    @FXML
    private void handleClickButtonMoveUp() {
        moveSelectedCardInTableView(-1);
    }

    /**
     * Handles click on button move down and moves selected card down
     */
    @FXML
    private void handleClickButtonMoveDown() {
        moveSelectedCardInTableView(1);
    }

    /**
     * Checks move buttons and set their disable state according indexes
     *
     * @param selectedIndex selectedIndex in table view
     * @param items         count of items in table view
     */
    private void checkMoveButtons(int selectedIndex, int items) {
        GUIHelpers.checkMoveButtons(selectedIndex, items, buttonMoveUp, buttonMoveDown);
    }

    /**
     * Returns active table view
     *
     * @return active table view
     *///
    private TableView<FitsCard> getSelectedTableView() {
        return getTableViewInTab(getSelectedTab(tabPaneDatas));
    }

    /**
     * Returns selected index in active table view
     *
     * @return selected index in active table view, -1 means no selected row in table view
     */
    private int getSelectedIndexInTableView(TableView<FitsCard> tableView) {
        return tableView.getSelectionModel().getSelectedIndex();
    }

    /**
     * Returns selected item in active table view
     *
     * @return selected item in active table view, null means no selected row in table view
     */
    private FitsCard getSelectedCardInTableView(TableView<FitsCard> tableView) {
        return tableView.getSelectionModel().getSelectedItem();
    }

    /**
     * Returns selected tab from given tab pane
     *
     * @param tabPane tab pane for searching selected tab
     * @return selected tab
     */
    private Tab getSelectedTab(TabPane tabPane) {
        return tabPane.getSelectionModel().getSelectedItem();
    }

    /**
     * Returns tab with given name from list of tabs
     *
     * @param tabs list of tabs, where I want to search
     * @param name name of tab, which I want to find
     * @return found tab, in case of no tab with given name is in list of tabs returns null
     */
    private Tab getTabWithName(List<Tab> tabs, String name) {
        if (name.trim().isEmpty()) {
            name = BLANK_CARD_TAB_NAME;
        }
        for (Tab tab : tabs) {
            if (tab.getText().equals(name)) {
                return tab;
            }
        }
        return null;
    }

    /**
     * Returns table view from given tab
     *
     * @param tab tab where it should find table view
     * @return table view from given tab
     */
    @SuppressWarnings("unchecked")
    private TableView<FitsCard> getTableViewInTab(Tab tab) {
        return (TableView<FitsCard>) tab.getContent();
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
     * Handles click on toggle files mode and change tab panes according to selected mode
     */
    @FXML
    private void handleClickToggleModeFiles() {
        FitsCard selectedCard = getSelectedCardInTableView(getSelectedTableView());
        Tab selectTab = null;
        tabPaneDatas.getTabs().clear();
        if (toggleGroupMode.getSelectedToggle() == toggleModeFiles) {
            tabPaneDatas.getTabs().addAll(tabsFiles);
            tabsCards.removeAll(tabsCardsToRemove);
            tabsCardsToRemove.clear();
            if (selectedCard != null) {
                selectTab = getTabWithName(tabsFiles, cardsInFiles.get(selectedCard).getFilename());
            }
        } else {
            tabPaneDatas.getTabs().addAll(tabsCards);
            if (selectedCard != null) {
                selectTab = getTabWithName(tabsCards, selectedCard.getKeywordName());
            }
        }
        if (selectTab != null) {
            tabPaneDatas.getSelectionModel().select(selectTab);
            getTableViewInTab(selectTab).getSelectionModel().select(selectedCard);
            getTableViewInTab(selectTab).scrollTo(selectedCard);
        }
        for (Tab tab : tabPaneDatas.getTabs()) {
            refreshContentTableView(getTableViewInTab(tab).getColumns().get(0));
        }
    }

    /**
     * Set disable state for delete buttons
     *
     * @param disable should be buttons disabled?
     */
    private void setDisableDeleteButtons(boolean disable) {
        buttonDeleteCard.setDisable(disable);
    }

    /**
     * Moves selected card in table view with given offset
     *
     * @param offset how many lines card should be moved
     */
    private void moveSelectedCardInTableView(int offset) {
        TableView<FitsCard> tableView = getSelectedTableView();
        int selectedIndex = getSelectedIndexInTableView(tableView);
        FitsCard movedCard = tableView.getItems().set(selectedIndex + offset, getSelectedCardInTableView(tableView));
        tableView.getItems().set(selectedIndex, movedCard);
    }

    /**
     * Refreshes content in table view of given column - javafx does not auto update view of values after committing edit
     *
     * @param column column in table view that you want to refresh
     */
    private void refreshContentTableView(TableColumn<FitsCard, ?> column) {
        column.setVisible(false);
        column.setVisible(true);
    }

    /**
     * Returns edit tab instance
     *
     * @return edit tab instance
     * @throws IOException in case of failure of loading new tab, but it should not occur
     */
    private Tab newEditTab() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(EDIT_TAB)); // New tab needs new instance of FXMLLoader
        return fxmlLoader.load();
    }

    /**
     * Close entire app
     */
    private void quit() {
        fitsFiles.forEach(FitsFile::close);
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
     * Opens dialog for changing date time values
     */
    private void openChangeDateTimeValueDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/ChangeTimeValue.fxml"));
            VBox vBox = loader.load();
            Stage dateTimeChanger = new Stage();
            TableView<FitsCard> tableView = getSelectedTableView();
            FitsCard card = getSelectedCardInTableView(tableView);
            dateTimeChanger.setTitle("Change date time value of Card " + card.getKeywordName());
            dateTimeChanger.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(vBox);
            dateTimeChanger.setScene(scene);
            dateTimeChanger.setMinWidth(686);
            dateTimeChanger.setMinHeight(203);
            dateTimeChanger.setMaxWidth(686);
            dateTimeChanger.setMaxHeight(203);
            GUIHelpers.setIcons(dateTimeChanger);
            ChangeTimeValueController controller = loader.getController();
            controller.prepareWindow(card, dateTimeChanger);
            dateTimeChanger.showAndWait();
            refreshContentTableView(tableView.getColumns().get(RVALUE_COLUMN));
        } catch (IOException ignored) {
        }
    }

    /**
     * Handles click on menu item Concatenation manager - opens the manager
     */
    private void openConcatenationManagerDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/ConcatenationManager.fxml"));
            VBox vBox = loader.load();
            Stage concatenationManager = new Stage();
            TableView<FitsCard> tableView = getSelectedTableView();
            concatenationManager.setTitle("Concatenation manager");
            concatenationManager.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(vBox);
            concatenationManager.setScene(scene);
            concatenationManager.setMinWidth(720);
            concatenationManager.setMinHeight(439);
            GUIHelpers.setIcons(concatenationManager);
            ConcatenationManagerController controller = loader.getController();
            controller.prepareWindow(tableView.getItems(), cardsInFiles, getSelectedCardInTableView(tableView), toggleGroupMode.getSelectedToggle() != toggleModeFiles, concatenationManager);
            concatenationManager.showAndWait();
            Pair<FitsCard, String> result = controller.getResult();
            if (result != null) {
                FitsCard card = result.getKey();
                if (card == null) {
                    card = handleClickButtonNewCard();
                }
                tableView.getSelectionModel().select(card);
                tableView.scrollTo(card);
                card.setRValue(result.getValue());
                refreshContentTableView(tableView.getColumns().get(RVALUE_COLUMN));
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Switches mode to multiple operation
     */
    private void switchMode() {
        GUIHelpers.showAlert(AlertType.CONFIRMATION, "Confirm switching mode", "Switching modes", "Are you sure you want to switch modes? You lost all unsaved changes.").ifPresent(response -> {
            if (response == ButtonType.OK) {
                fitsFiles.forEach(FitsFile::close);
                MultipleEditController multipleEditController = (MultipleEditController) mainViewController.setContent("fxml/MultipleEdit.fxml");
                deactivateMenuItems();
                multipleEditController.prepareWindow(files, mainViewController);
            }
        });
    }

    /**
     * Deactivates menu items for single operation mode
     */
    private void deactivateMenuItems() {
        GUIHelpers.modifyMenuItem(mainViewController, MenuBarController.MENU_TOOLS, MenuBarController.MENU_ITEM_TOOLS_CONCATENATION_MANAGER, true, null);
    }
}
