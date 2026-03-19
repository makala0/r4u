package cz.be.r4u;

import cz.be.r4u.ui.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApp extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(R4uApplication.class)
                .headless(false)
                .run();
    }

    @Override
    public void start(Stage stage) {
        SceneManager sceneManager = context.getBean(SceneManager.class);
        sceneManager.setPrimaryStage(stage);
        sceneManager.showLogin();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }
}