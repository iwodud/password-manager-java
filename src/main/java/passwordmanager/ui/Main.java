package passwordmanager.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import passwordmanager.logic.PasswordManager;
import passwordmanager.logic.MasterPasswordManager;
import passwordmanager.model.AccountEntry;

import java.util.List;

public class Main extends Application {

    private PasswordManager passwordManager = new PasswordManager();
    private MasterPasswordManager masterPasswordManager = new MasterPasswordManager();

    private Button editButton = new Button("Edit");
    private Button deleteButton = new Button("Delete");

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
                    new Alert(Alert.AlertType.INFORMATION, "Password set. Please log in.").showAndWait();
                    showLoginScreen(stage);
                }
            });

            loginScene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) setButton.fire();
            });

        } else {
            Label label = new Label("Enter master password:");
            PasswordField passwordField = new PasswordField();
            Button loginButton = new Button("Login");

            loginLayout.getChildren().addAll(label, passwordField, loginButton);

            loginButton.setOnAction(e -> {
                if (masterPasswordManager.verifyPassword(passwordField.getText())) {
                    showMainApp(stage);
                } else {
                    new Alert(Alert.AlertType.ERROR, "Invalid master password").showAndWait();
                }
            });

            loginScene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) loginButton.fire();
            });
        }
    }

    private void showMainApp(Stage stage) {
        passwordManager.loadFromFile();

        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20;");

        ListView<String> listView = new ListView<>();

        final AccountEntry[] selectedEntry = {null};
        final AccountEntry[] editingEntry = {null};

        TextField platformField = new TextField();
        platformField.setPromptText("Platform");

        TextField loginField = new TextField();
        loginField.setPromptText("Login");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        TextField visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("Password");
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        Button togglePasswordButton = new Button("Show");
        Button addButton = new Button("Add");
        Button exitButton = new Button("Exit");

        editButton.setDisable(true);
        deleteButton.setDisable(true);

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int index = listView.getSelectionModel().getSelectedIndex();
                List<AccountEntry> entries = passwordManager.getAllEntries();

                if (index >= 0 && index < entries.size()) {
                    AccountEntry entry = entries.get(index);

                    Dialog<Void> dialog = new Dialog<>();
                    dialog.setTitle("Account Details");
                    dialog.setHeaderText(entry.getPlatform());

                    Label loginLabel = new Label("Login: " + entry.getLogin());
                    Label passwordLabel = new Label("Password: " + entry.getPassword());

                    Button copyButton = new Button("Copy Password");
                    copyButton.setOnAction(ev -> {
                        ClipboardContent content = new ClipboardContent();
                        content.putString(entry.getPassword());
                        Clipboard.getSystemClipboard().setContent(content);
                        new Alert(Alert.AlertType.INFORMATION, "Password copied").showAndWait();
                    });

                    VBox box = new VBox(10, loginLabel, passwordLabel, copyButton);
                    box.setStyle("-fx-padding: 10;");

                    dialog.getDialogPane().setContent(box);
                    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                    dialog.showAndWait();
                }
            }
        });

        togglePasswordButton.setOnAction(e -> {
            boolean show = togglePasswordButton.getText().equals("Show");
            visiblePasswordField.setText(passwordField.getText());
            passwordField.setText(visiblePasswordField.getText());

            visiblePasswordField.setVisible(show);
            visiblePasswordField.setManaged(show);
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);

            togglePasswordButton.setText(show ? "Hide" : "Show");
        });

        addButton.setOnAction(e -> {
            String platform = platformField.getText();
            String login = loginField.getText();
            String password = passwordField.isVisible()
                    ? passwordField.getText()
                    : visiblePasswordField.getText();

            if (!platform.isEmpty() && !login.isEmpty() && !password.isEmpty()) {
                if (editingEntry[0] != null) {
                    editingEntry[0].setPlatform(platform);
                    editingEntry[0].setLogin(login);
                    editingEntry[0].setPassword(password);
                    editingEntry[0] = null;
                    addButton.setText("Add");
                } else {
                    passwordManager.addEntry(new AccountEntry(platform, login, password));
                }

                passwordManager.saveToFile();
                refreshList(listView, selectedEntry);

                platformField.clear();
                loginField.clear();
                passwordField.clear();
                visiblePasswordField.clear();
            }
        });

        editButton.setOnAction(e -> {
            if (selectedEntry[0] != null) {
                AccountEntry entry = selectedEntry[0];
                platformField.setText(entry.getPlatform());
                loginField.setText(entry.getLogin());
                passwordField.setText(entry.getPassword());
                visiblePasswordField.setText(entry.getPassword());
                editingEntry[0] = entry;
                addButton.setText("Save");
            }
        });

        deleteButton.setOnAction(e -> {
            if (selectedEntry[0] != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setHeaderText("Delete entry?");
                alert.setContentText(selectedEntry[0].getPlatform());

                alert.showAndWait().ifPresent(r -> {
                    if (r == ButtonType.OK) {
                        passwordManager.removeEntry(selectedEntry[0]);
                        passwordManager.saveToFile();
                        refreshList(listView, selectedEntry);
                    }
                });
            }
        });

        exitButton.setOnAction(e -> stage.close());

        HBox passwordBox = new HBox(10, passwordField, visiblePasswordField, togglePasswordButton);
        HBox.setHgrow(passwordField, Priority.ALWAYS);
        HBox.setHgrow(visiblePasswordField, Priority.ALWAYS);
        togglePasswordButton.setPrefWidth(60);

        HBox buttonBox = new HBox(10, addButton, editButton, deleteButton, exitButton);

        layout.getChildren().addAll(
                new Label("Saved Accounts:"),
                listView,
                platformField,
                loginField,
                passwordBox,
                buttonBox
        );

        Scene scene = new Scene(layout, 400, 500);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) addButton.fire();
        });

        stage.setScene(scene);
        stage.setTitle("Password Manager");
        stage.show();

        refreshList(listView, selectedEntry);
    }

    private void refreshList(ListView<String> listView, AccountEntry[] selectedEntryRef) {
        listView.getItems().clear();
        List<AccountEntry> entries = passwordManager.getAllEntries();

        for (AccountEntry e : entries) {
            listView.getItems().add(e.getPlatform() + " - " + e.getLogin());
        }

        listView.getSelectionModel().selectedIndexProperty().addListener((o, oldVal, newVal) -> {
            int i = newVal.intValue();
            if (i >= 0 && i < entries.size()) {
                selectedEntryRef[0] = entries.get(i);
                editButton.setDisable(false);
                deleteButton.setDisable(false);
            } else {
                selectedEntryRef[0] = null;
                editButton.setDisable(true);
                deleteButton.setDisable(true);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
