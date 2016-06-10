package cz.muni.sci.astro.fhm.gui;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Contains helper functions
 *
 * @author Jan Hlava, 395986
 */
public class GUIHelpers {
    private static final List<Image> icons;

    static {
        icons = new ArrayList<>();
        icons.add(new Image(App.class.getResourceAsStream("icons/icon128x128.png")));
        icons.add(new Image(App.class.getResourceAsStream("icons/icon64x64.png")));
        icons.add(new Image(App.class.getResourceAsStream("icons/icon48x48.png")));
        icons.add(new Image(App.class.getResourceAsStream("icons/icon32x32.png")));
        icons.add(new Image(App.class.getResourceAsStream("icons/icon16x16.png")));
    }

    /**
     * Shows alert of given type
     *
     * @param alertType alert type
     * @param title     title text in alert
     * @param header    header text in alert
     * @param content   content text in alert
     * @return returns result of dialog
     */
    public static Optional<ButtonType> showAlert(AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().addAll(icons);
        return alert.showAndWait();
    }

    /**
     * Checks move buttons and set their disable state according indexes
     *
     * @param selectedIndex  selected index of rows
     * @param items          count of rows
     * @param buttonMoveUp   button with action move up
     * @param buttonMoveDown button with action move down
     */
    public static void checkMoveButtons(int selectedIndex, int items, Button buttonMoveUp, Button buttonMoveDown) {
        if (selectedIndex == -1 || (selectedIndex == 0 && items == 1)) {
            buttonMoveDown.setDisable(true);
            buttonMoveUp.setDisable(true);
        } else if (selectedIndex == 0) {
            buttonMoveDown.setDisable(false);
            buttonMoveUp.setDisable(true);
        } else if (selectedIndex + 1 == items) {
            buttonMoveDown.setDisable(true);
            buttonMoveUp.setDisable(false);
        } else {
            buttonMoveDown.setDisable(false);
            buttonMoveUp.setDisable(false);
        }
    }

    /**
     * Set icons for given stage
     *
     * @param stage stage for setting icons
     */
    public static void setIcons(Stage stage) {
        stage.getIcons().addAll(icons);
    }

    /**
     * Modifies given menu item in given menu - sets disable state and event set on action
     *
     * @param mainViewController mainViewController containing menus
     * @param menuIndex          index of menu in menu bar
     * @param menuItemIndex      index of menu item in menu
     * @param disable            should be menu item disabled?
     * @param setOnAction        event for OnAction
     */
    public static void modifyMenuItem(MainViewController mainViewController, int menuIndex, int menuItemIndex, boolean disable, EventHandler<ActionEvent> setOnAction) {
        MenuItem menuItem = mainViewController.getMenuBar().getMenus().get(menuIndex).getItems().get(menuItemIndex);
        menuItem.setDisable(disable);
        menuItem.setOnAction(setOnAction);
    }

    /**
     * Creates progress information window about running operation
     *
     * @param processInfo information about process
     * @param task        task of process
     * @return stage of created window
     */
    public static Stage createProgressInfo(String processInfo, Task<?> task) {
        try {
            FXMLLoader loader = new FXMLLoader(GUIHelpers.class.getResource("fxml/ProgressInfo.fxml"));
            VBox vBox = loader.load();
            Stage progressInfo = new Stage();
            progressInfo.setTitle("Progress monitor");
            progressInfo.initStyle(StageStyle.UTILITY);
            progressInfo.setOnCloseRequest(Event::consume);
            progressInfo.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(vBox);
            progressInfo.setMinHeight(130);
            progressInfo.setMinWidth(600);
            progressInfo.setScene(scene);
            progressInfo.sizeToScene();
            setIcons(progressInfo);
            ProgressInfoController controller = loader.getController();
            controller.prepareWindow(processInfo, task);
            return progressInfo;
        } catch (IOException ignored) {
            return new Stage();
        }
    }
}
