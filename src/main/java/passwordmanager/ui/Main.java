package passwordmanager.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import passwordmanager.logic.PasswordManager;
import passwordmanager.logic.MasterPasswordManager;
import passwordmanager.model.AccountEntry;

import java.util.List;

public class Main extends Application {
    private PasswordManager passwordManager = new PasswordManager();
    private MasterPasswordManager masterPasswordManager = new MasterPasswordManager();

    @Override
    public void start(Stage primaryStage) {
        showLoginScreen(primaryStage);
    }

    private void showLoginScreen(Stage stage) {
        VBox loginLayout = new VBox(10);
        loginLayout.setStyle("-fx-padding: 20; -fx-alignment: center;");

        Scene loginScene = new Scene(loginLayout, 300, 180);
        stage.setScene(loginScene);
        stage.setTitle("Password Manager - Login");
        stage.show();

        if (!masterPasswordManager.isPasswordSet()) {
            Label label = new Label("Set master password:");
            PasswordField newPasswordField = new PasswordField();
            Button setButton = new Button("Set Password");

            loginLayout.getChildren().addAll(label, newPasswordField, setButton);

            setButton.setOnAction(e -> {
                String newPassword = newPasswordField.getText();
                if (!newPassword.isEmpty()) {
                    masterPasswordManager.setPassword(newPassword);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Password set. Please log in.", ButtonType.OK);
                    alert.showAndWait();
                    showLoginScreen(stage); // reload login screen
                }
            });

            loginScene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    setButton.fire();
                }
            });

        } else {
            Label label = new Label("Enter master password:");
            PasswordField passwordField = new PasswordField();
            Button loginButton = new Button("Login");

            loginLayout.getChildren().addAll(label, passwordField, loginButton);

            loginButton.setOnAction(e -> {
                String inputPassword = passwordField.getText();
                if (masterPasswordManager.verifyPassword(inputPassword)) {
                    showMainApp(stage);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid master password", ButtonType.OK);
                    alert.showAndWait();
                }
            });

            loginScene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    loginButton.fire();
                }
            });
        }
    }

    private void showMainApp(Stage stage) {
        passwordManager.loadFromFile();

        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20;");

        ListView<String> listView = new ListView<>();
        refreshList(listView);

        TextField platformField = new TextField();
        platformField.setPromptText("Platform (e.g. Gmail)");

        TextField loginField = new TextField();
        loginField.setPromptText("Login");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button addButton = new Button("Add");

        addButton.setOnAction(e -> {
            String platform = platformField.getText();
            String login = loginField.getText();
            String password = passwordField.getText();

            if (!platform.isEmpty() && !login.isEmpty() && !password.isEmpty()) {
                passwordManager.addEntry(new AccountEntry(platform, login, password));
                passwordManager.saveToFile();
                refreshList(listView);

                platformField.clear();
                loginField.clear();
                passwordField.clear();
            }
        });

        Scene scene = new Scene(layout, 400, 500);
        layout.getChildren().addAll(
                new Label("Saved Accounts:"),
                listView,
                new Label("Add New Entry:"),
                platformField,
                loginField,
                passwordField,
                addButton
        );

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                addButton.fire();
            }
        });

        stage.setScene(scene);
        stage.setTitle("Password Manager");
        stage.show();
    }

    private void refreshList(ListView<String> listView) {
        listView.getItems().clear();
        List<AccountEntry> entries = passwordManager.getAllEntries();
        for (AccountEntry entry : entries) {
            listView.getItems().add(entry.getPlatform() + " - " + entry.getLogin());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
