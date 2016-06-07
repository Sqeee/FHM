package cz.muni.sci.astro.fhm.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for menu bar
 *
 * @author Jan Hlava, 395986
 */
public class MenuBarController {
    public static final int MENU_FILE = 0;
    public static final int MENU_ITEM_FILE_SAVE = 0;
    public static final int MENU_ITEM_FILE_EXIT = 1;
    public static final int MENU_MODES = 1;
    public static final int MENU_ITEM_MODES_SINGLE = 0;
    public static final int MENU_ITEM_MODES_MULTIPLE = 1;
    public static final int MENU_TOOLS = 2;
    public static final int MENU_ITEM_TOOLS_CONCATENATION_MANAGER = 1;

    @FXML
    private MenuBar menuBar;

    /**
     * Handles click on menu item Exit - closes this app
     */
    @FXML
    private void handleClickMenuItemExit() {
        Platform.exit();
        System.exit(0);
    }

    /**
     * Handles click on menu item About - prints short description about this app
     */
    @FXML
    private void handleClickMenuItemAbout() {
        GUIHelpers.showAlert(AlertType.INFORMATION, "About", "File Header Modifier", "This application provides tools to modify FITS headers. You can open multiple files from one directory, filter them by name or content. After choosing files you can edit card images in header.\n\nContact e-mail: honzahlava@mail.muni.cz");
    }

    /**
     * Handles click on menu item UTC and JD converter - shows window with converting options
     */
    @FXML
    private void handleClickMenuItemUTC_JD_Converter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/Converter.fxml"));
            VBox vBox = loader.load();
            Stage converter = new Stage();
            converter.setTitle("Converter");
            Scene scene = new Scene(vBox);
            converter.setScene(scene);
            converter.setMinWidth(616);
            converter.setMinHeight(178);
            converter.setMaxWidth(616);
            converter.setMaxHeight(178);
            GUIHelpers.setIcons(converter);
            converter.show();
        } catch (IOException ignored) {
        }
    }

    /**
     * Handles click on menu item Help - shows window with help
     */
    @FXML
    private void handleClickMenuItemHelp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/Help.fxml"));
            AnchorPane anchorPane = loader.load();
            Stage help = new Stage();
            help.setTitle("Help");
            Scene scene = new Scene(anchorPane);
            help.setScene(scene);//TODO
            /*converter.setMinWidth(616);
            converter.setMinHeight(178);
            converter.setMaxWidth(616);
            converter.setMaxHeight(178);*/
            GUIHelpers.setIcons(help);
            help.show();
        } catch (IOException ignored) {
        }
    }
}
