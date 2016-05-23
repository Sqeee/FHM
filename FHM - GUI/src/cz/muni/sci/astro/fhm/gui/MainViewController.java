package cz.muni.sci.astro.fhm.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.Pane;

import java.io.IOException;

/**
 * Controller for Main form containing content and menu bar
 *
 * @author Jan Hlava, 395986
 */
public class MainViewController
{
    @FXML private SelectFiles1Controller selectFiles1Controller;
    @FXML private Pane content;
    @FXML private MenuBar menuBar;

    /**
     * Sets new main content loaded from given FXML document
     *
     * @param fxmlFilename filename to FXML document for loading
     * @return controller of newly loaded FXML document (null in case of error)
     */
    public Object setContent(String fxmlFilename)
    {
        content.getChildren().clear();
        try
        {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFilename));
            content.getChildren().clear();
            content.getChildren().add(fxmlLoader.load());
            return fxmlLoader.getController();
        }
        catch (IOException exc)
        {
            return null;
        }
    }

    /**
     * Gets app menu bar
     *
     * @return app menu bar
     */
    public MenuBar getMenuBar()
    {
        return menuBar;
    }

    /**
     * Initializes this controller
     */
    @FXML
    public void initialize()
    {
        selectFiles1Controller.setMainViewController(this);
    }
}
