package cz.muni.sci.astro.fhm.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main class for GUI
 *
 * @author Jan Hlava, 395986
 */
public class App extends Application
{
    /**
     * Starts GUI
     *
     * @param primaryStage stage for showing FXML documents
     * @throws Exception exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        Parent root = FXMLLoader.load(getClass().getResource("MainView.fxml"));
        primaryStage.setMinHeight(560);
        primaryStage.setMinWidth(750);
        primaryStage.setTitle("Fits Header Modifier");
        primaryStage.setScene(new Scene(root, 750, 560));
        GUIHelpers.setIcons(primaryStage);
        primaryStage.show();
    }

    /**
     * Launch GUI
     *
     * @param args array containing params from command line
     */
    public static void main(String[] args)
    {
        launch(args);
    }
}
