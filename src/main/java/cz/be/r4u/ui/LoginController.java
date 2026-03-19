package cz.be.r4u.ui;

import cz.be.r4u.auth.AuthService;
import cz.be.r4u.entity.UserAccount;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

@Component
public class LoginController {

    private final AuthService authService;
    private final SceneManager sceneManager;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    public LoginController(AuthService authService, SceneManager sceneManager) {
        this.authService = authService;
        this.sceneManager = sceneManager;
    }

    @FXML
    public void onLoginClick() {
        try {
            UserAccount user = authService.login(
                    usernameField.getText(),
                    passwordField.getText()
            );

            messageLabel.setText("");
            sceneManager.showHome(user);
        } catch (IllegalArgumentException ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    public void onGoToRegisterClick() {
        messageLabel.setText("");
        sceneManager.showRegister();
    }
}