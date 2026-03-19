package cz.be.r4u.ui;

import java.io.IOException;

import cz.be.r4u.entity.UserAccount;
import cz.be.r4u.service.DashboardService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SceneManager {

    private final ApplicationContext applicationContext;
    private Stage primaryStage;

    public SceneManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("R4U");
        this.primaryStage.setResizable(true);
        applyWindowMode();
    }

    public void showLogin() {
        setScene("/fxml/login.fxml");
    }

    public void showRegister() {
        setScene("/fxml/register.fxml");
    }

    public void showHome(UserAccount userAccount) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            HomeController controller = loader.getController();
            controller.setLoggedUser(userAccount);

            showRoot(root);
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se načíst home.fxml", e);
        }
    }

    public void showDashboard(UserAccount userAccount) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            DashboardController controller = loader.getController();
            controller.setLoggedUser(userAccount);

            showRoot(root);
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se načíst dashboard.fxml", e);
        }
    }

    public void showRoleDetail(UserAccount userAccount, DashboardService.DashboardRow selectedRoll) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/role-detail.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            RoleDetailController controller = loader.getController();
            controller.setContext(userAccount, selectedRoll);

            showRoot(root);
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se načíst role-detail.fxml", e);
        }
    }

    public void showRejectRules(UserAccount userAccount) {
        showRejectRules(userAccount, null);
    }

    public void showRejectRules(UserAccount userAccount, DashboardService.DashboardRow selectedRoll) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reject-rules.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            RejectRulesController controller = loader.getController();
            controller.setContext(userAccount, selectedRoll);

            showRoot(root);
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se načíst reject-rules.fxml", e);
        }
    }

    private void setScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            showRoot(root);
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se načíst FXML: " + fxmlPath, e);
        }
    }

    private void showRoot(Parent root) {
        Scene existingScene = primaryStage.getScene();

        if (existingScene == null) {
            primaryStage.setScene(new Scene(root));
        } else {
            existingScene.setRoot(root);
        }

        applyWindowMode();
        primaryStage.show();
    }

    private void applyWindowMode() {
        primaryStage.setMaximized(true);
    }
}