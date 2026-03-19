package cz.be.r4u.ui;

import cz.be.r4u.entity.UserAccount;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

@Component
public class HomeController {

    private final SceneManager sceneManager;

    @FXML
    private Label welcomeLabel;

    private UserAccount loggedUser;

    public HomeController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public void setLoggedUser(UserAccount loggedUser) {
        this.loggedUser = loggedUser;
        welcomeLabel.setText("Vítej, " + loggedUser.getUsername() + "!");
    }

    @FXML
    public void onOpenDashboardClick() {
        sceneManager.showDashboard(loggedUser);
    }

    @FXML
    public void onLogoutClick() {
        loggedUser = null;
        sceneManager.showLogin();
    }
}