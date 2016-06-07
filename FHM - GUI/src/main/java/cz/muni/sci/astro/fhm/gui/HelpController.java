package cz.muni.sci.astro.fhm.gui;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;

/**
 * Controller for help section
 *
 * @author Jan Hlava, 395986
 */
public class HelpController {
    @FXML
    private WebView webViewHelp;

    /**
     * Initializes this controller
     */
    @FXML
    public void initialize() {
        webViewHelp.getEngine().load(getClass().getResource("help.html").toExternalForm());
    }
}
