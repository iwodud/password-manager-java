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
        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");

        Scene scene = new Scene(layout, 300, 180);
        stage.setScene(scene);
        stage.setTitle("Password Manager - Login");
        stage.show();

        if (!masterPasswordManager.isPasswordSet()) {
            Label label = new Label("Set master password:");
            PasswordField field = new PasswordField();
            Button button = new Button("Set Password");

            layout.getChildren().addAll(label, field, button);

            button.setOnAction(e -> {
                if (!field.getText().isEmpty()) {
                    masterPasswordManager.setPassword(field.getText());
                    new Alert(Alert.AlertType.INFORMATION, "Password set. Please log in.").showAndWait();
                    showLoginScreen(stage);
                }
            });

            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) button.fire();
            });

        } else {
            Label label = new Label("Enter master password:");
            PasswordField field = new PasswordField();
            Button button = new Button("Login");

            layout.getChildren().addAll(label, field, button);

            button.setOnAction(e -> {
                if (masterPasswordManager.verifyPassword(field.getText())) {
                    showMainApp(stage);
                } else {
                    new Alert(Alert.AlertType.ERROR, "Invalid master password").showAndWait();
                }
            });

            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) button.fire();
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

        Button toggleButton = new Button("Show");
        Button generateButton = new Button("Generate Password");
        Button addButton = new Button("Add");
        Button exitButton = new Button("Exit");

        editButton.setDisable(true);
        deleteButton.setDisable(true);

        toggleButton.setOnAction(e -> {
            boolean show = toggleButton.getText().equals("Show");
            visiblePasswordField.setText(passwordField.getText());
            passwordField.setText(visiblePasswordField.getText());

            visiblePasswordField.setVisible(show);
            visiblePasswordField.setManaged(show);
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);

            toggleButton.setText(show ? "Hide" : "Show");
        });

        generateButton.setOnAction(e -> {
            String generated = generateStrongPassword(12);
            passwordField.setText(generated);
            visiblePasswordField.setText(generated);
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

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int index = listView.getSelectionModel().getSelectedIndex();
                List<AccountEntry> entries = passwordManager.getAllEntries();

                if (index >= 0 && index < entries.size()) {
                    AccountEntry entry = entries.get(index);

                    Dialog<Void> dialog = new Dialog<>();
                    dialog.setTitle("Account Details");
                    dialog.setHeaderText(entry.getPlatform());

                    Label l1 = new Label("Login: " + entry.getLogin());
                    Label l2 = new Label("Password: " + entry.getPassword());

                    Button copy = new Button("Copy Password");
                    copy.setOnAction(ev -> {
                        ClipboardContent c = new ClipboardContent();
                        c.putString(entry.getPassword());
                        Clipboard.getSystemClipboard().setContent(c);
                        new Alert(Alert.AlertType.INFORMATION, "Password copied").showAndWait();
                    });

                    VBox box = new VBox(10, l1, l2, copy);
                    box.setStyle("-fx-padding: 10;");
                    dialog.getDialogPane().setContent(box);
                    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                    dialog.showAndWait();
                }
            }
        });

        HBox passwordBox = new HBox(10, passwordField, visiblePasswordField, toggleButton);
        HBox.setHgrow(passwordField, Priority.ALWAYS);
        HBox.setHgrow(visiblePasswordField, Priority.ALWAYS);

        HBox leftButtons = new HBox(10, addButton, editButton, deleteButton, generateButton);
        HBox rightButtons = new HBox(exitButton);
        rightButtons.setStyle("-fx-alignment: center-right;");
        HBox bottomRow = new HBox(10, leftButtons, rightButtons);
        HBox.setHgrow(rightButtons, Priority.ALWAYS);

        layout.getChildren().addAll(
                new Label("Saved Accounts:"),
                listView,
                platformField,
                loginField,
                passwordBox,
                bottomRow
        );

        Scene scene = new Scene(layout, 420, 520);
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

        listView.getSelectionModel().selectedIndexProperty().addListener((o, oldV, newV) -> {
            int i = newV.intValue();
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

    private String generateStrongPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[]{}";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
