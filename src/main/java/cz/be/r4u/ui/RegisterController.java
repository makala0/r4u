package cz.be.r4u.ui;

import cz.be.r4u.auth.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

@Component
public class RegisterController {

    private final AuthService authService;
    private final SceneManager sceneManager;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    public RegisterController(AuthService authService, SceneManager sceneManager) {
        this.authService = authService;
        this.sceneManager = sceneManager;
    }

    @FXML
    public void onRegisterClick() {
        try {
            authService.register(
                    usernameField.getText(),
                    emailField.getText(),
                    passwordField.getText(),
                    confirmPasswordField.getText()
            );

            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Registrace proběhla úspěšně. Teď se můžeš přihlásit.");
        } catch (IllegalArgumentException ex) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    public void onBackToLoginClick() {
        messageLabel.setText("");
        sceneManager.showLogin();
    }
}