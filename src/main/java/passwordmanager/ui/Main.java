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

            setButton.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                @Override
                public void handle(javafx.event.ActionEvent event) {
                    String newPassword = newPasswordField.getText();
                    if (!newPassword.isEmpty()) {
                        masterPasswordManager.setPassword(newPassword);
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Password set. Please log in.", ButtonType.OK);
                        alert.showAndWait();
                        showLoginScreen(stage);
                    }
                }
            });

            loginScene.setOnKeyPressed(new javafx.event.EventHandler<javafx.scene.input.KeyEvent>() {
                @Override
                public void handle(javafx.scene.input.KeyEvent event) {
                    if (event.getCode() == KeyCode.ENTER) {
                        setButton.fire();
                    }
                }
            });

        } else {
            Label label = new Label("Enter master password:");
            PasswordField passwordField = new PasswordField();
            Button loginButton = new Button("Login");

            loginLayout.getChildren().addAll(label, passwordField, loginButton);

            loginButton.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                @Override
                public void handle(javafx.event.ActionEvent event) {
                    String inputPassword = passwordField.getText();
                    if (masterPasswordManager.verifyPassword(inputPassword)) {
                        showMainApp(stage);
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid master password", ButtonType.OK);
                        alert.showAndWait();
                    }
                }
            });

            loginScene.setOnKeyPressed(new javafx.event.EventHandler<javafx.scene.input.KeyEvent>() {
                @Override
                public void handle(javafx.scene.input.KeyEvent event) {
                    if (event.getCode() == KeyCode.ENTER) {
                        loginButton.fire();
                    }
                }
            });
        }
    }

    private void showMainApp(Stage stage) {
        passwordManager.loadFromFile();

        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20;");

        ListView<String> listView = new ListView<>();
        final AccountEntry[] selectedEntry = {null};
        refreshList(listView, selectedEntry);

        TextField platformField = new TextField();
        platformField.setPromptText("Platform (e.g. Gmail)");

        TextField loginField = new TextField();
        loginField.setPromptText("Login");

        PasswordField passwordField = new PasswordField();
        TextField visiblePasswordField = new TextField();
        Button togglePasswordButton = new Button("Show");

        passwordField.setPromptText("Password");
        visiblePasswordField.setPromptText("Password");
        visiblePasswordField.setManaged(false);
        visiblePasswordField.setVisible(false);

        togglePasswordButton.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                if (togglePasswordButton.getText().equals("Show")) {
                    visiblePasswordField.setText(passwordField.getText());
                    visiblePasswordField.setVisible(true);
                    visiblePasswordField.setManaged(true);
                    passwordField.setVisible(false);
                    passwordField.setManaged(false);
                    togglePasswordButton.setText("Hide");
                } else {
                    passwordField.setText(visiblePasswordField.getText());
                    passwordField.setVisible(true);
                    passwordField.setManaged(true);
                    visiblePasswordField.setVisible(false);
                    visiblePasswordField.setManaged(false);
                    togglePasswordButton.setText("Show");
                }
            }
        });

        Button addButton = new Button("Add");
        addButton.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                String platform = platformField.getText();
                String login = loginField.getText();
                String password = passwordField.isVisible() ? passwordField.getText() : visiblePasswordField.getText();

                if (!platform.isEmpty() && !login.isEmpty() && !password.isEmpty()) {
                    passwordManager.addEntry(new AccountEntry(platform, login, password));
                    passwordManager.saveToFile();
                    refreshList(listView, selectedEntry);
                    platformField.clear();
                    loginField.clear();
                    passwordField.clear();
                    visiblePasswordField.clear();
                }
            }
        });

        Button deleteButton = new Button("Delete Selected");
        deleteButton.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                if (selectedEntry[0] != null) {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Confirm Deletion");
                    confirmAlert.setHeaderText("Are you sure you want to delete this entry?");
                    confirmAlert.setContentText(selectedEntry[0].getPlatform() + " - " + selectedEntry[0].getLogin());

                    confirmAlert.showAndWait().ifPresent(new java.util.function.Consumer<ButtonType>() {
                        @Override
                        public void accept(ButtonType response) {
                            if (response == ButtonType.OK) {
                                passwordManager.removeEntry(selectedEntry[0]);
                                passwordManager.saveToFile();
                                refreshList(listView, selectedEntry);
                            }
                        }
                    });
                }
            }
        });

        HBox passwordBox = new HBox(10);
        passwordField.setPrefWidth(250);
        visiblePasswordField.setPrefWidth(250);
        togglePasswordButton.setPrefWidth(60);

        HBox.setHgrow(passwordField, Priority.ALWAYS);
        HBox.setHgrow(visiblePasswordField, Priority.ALWAYS);

        passwordBox.getChildren().addAll(passwordField, visiblePasswordField, togglePasswordButton);

        layout.getChildren().addAll(
                new Label("Saved Accounts:"),
                listView,
                new Label("Add New Entry:"),
                platformField,
                loginField,
                passwordBox,
                addButton,
                deleteButton
        );

        Scene scene = new Scene(layout, 400, 500);

        scene.setOnKeyPressed(new javafx.event.EventHandler<javafx.scene.input.KeyEvent>() {
            @Override
            public void handle(javafx.scene.input.KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    addButton.fire();
                }
            }
        });

        stage.setScene(scene);
        stage.setTitle("Password Manager");
        stage.show();
    }

    private void refreshList(ListView<String> listView, AccountEntry[] selectedEntryRef) {
        listView.getItems().clear();
        List<AccountEntry> entries = passwordManager.getAllEntries();

        for (AccountEntry entry : entries) {
            String itemText = entry.getPlatform() + " - " + entry.getLogin();
            listView.getItems().add(itemText);
        }

        listView.getSelectionModel().selectedIndexProperty().addListener(new javafx.beans.value.ChangeListener<Number>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends Number> obs, Number oldVal, Number newVal) {
                int index = newVal.intValue();
                if (index >= 0 && index < entries.size()) {
                    selectedEntryRef[0] = entries.get(index);
                } else {
                    selectedEntryRef[0] = null;
                }
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
