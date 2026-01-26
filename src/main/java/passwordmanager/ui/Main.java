package passwordmanager.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import java.time.Duration;
import java.time.Instant;
import java.io.FileWriter;
import java.io.IOException;
import passwordmanager.logic.PasswordManager;
import passwordmanager.logic.MasterPasswordManager;
import passwordmanager.logic.CryptoUtils;
import passwordmanager.model.AccountEntry;

import java.util.List;

public class Main extends Application {
    private int failedAttempts = 0;
    private Instant lastFailedAttempt = null;
    private PasswordManager passwordManager = new PasswordManager();
    private MasterPasswordManager masterPasswordManager = new MasterPasswordManager();
    private String masterPassword;

    @Override
    public void start(Stage primaryStage) {
        showLoginScreen(primaryStage);
    }

    private void showLoginScreen(Stage stage) {
        VBox loginLayout = new VBox(10);
        loginLayout.setStyle("-fx-padding: 20; -fx-alignment: center;");

        Scene loginScene = new Scene(loginLayout, 300, 230);
        stage.setScene(loginScene);
        stage.setTitle("Password Manager - Login");
        stage.show();

        if (!masterPasswordManager.isPasswordSet()) {

            Label label = new Label("Set master password:");
            PasswordField newPasswordField = new PasswordField();
            newPasswordField.setPromptText("Password");

            Label strengthLabel = new Label("Password strength: ");
            strengthLabel.setStyle("-fx-font-weight: bold;");

            Button setButton = new Button("Set Password");

            loginLayout.getChildren().addAll(label, newPasswordField, strengthLabel, setButton);

            newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {

                if (newVal.length() < 6) {
                    strengthLabel.setText("Password strength: VERY WEAK");
                    strengthLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                }
                else if (!masterPasswordManager.isStrongPassword(newVal)) {
                    strengthLabel.setText("Password strength: MEDIUM");
                    strengthLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                }
                else {
                    strengthLabel.setText("Password strength: STRONG");
                    strengthLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                }
            });

            setButton.setOnAction(e -> {
                String newPassword = newPasswordField.getText();

                if (!masterPasswordManager.isStrongPassword(newPassword)) {
                    new Alert(Alert.AlertType.ERROR,
                            "Password is too weak!\n\n" +
                                    "Requirements:\n" +
                                    "- at least 8 characters\n" +
                                    "- one uppercase letter\n" +
                                    "- one lowercase letter\n" +
                                    "- one digit\n" +
                                    "- one special character").showAndWait();
                    return;
                }

                masterPasswordManager.setPassword(newPassword);
                new Alert(Alert.AlertType.INFORMATION,
                        "Strong password set.\nPlease log in.").showAndWait();
                showLoginScreen(stage);
            });

            loginScene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    setButton.fire();
                }
            });

        }

        else {

            Label label = new Label("Enter master password:");
            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Password");
            Button loginButton = new Button("Login");

            loginLayout.getChildren().addAll(label, passwordField, loginButton);

            loginButton.setOnAction(e -> {
                if (isLoginBlocked()) {
                    long remaining = getSecondsToWait() - Duration.between(lastFailedAttempt, Instant.now()).getSeconds();
                    if (remaining < 0) remaining = 0;

                    new Alert(Alert.AlertType.WARNING,
                            "Too many failed attempts.\nPlease wait " + formatWaitTime(remaining)).showAndWait();
                    return;
                }

                String inputPassword = passwordField.getText();
                if (masterPasswordManager.verifyPassword(inputPassword)) {
                    masterPassword = inputPassword;
                    failedAttempts = 0;
                    lastFailedAttempt = null;
                    showMainApp(stage);
                } else {
                    failedAttempts++;
                    lastFailedAttempt = Instant.now();
                    new Alert(Alert.AlertType.ERROR, "Invalid master password").showAndWait();
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
        visiblePasswordField.setManaged(false);
        visiblePasswordField.setVisible(false);

        Button togglePasswordButton = new Button("Show");
        Button generatePasswordButton = new Button("Generate Password");
        Button addButton = new Button("Add");
        Button editButton = new Button("Edit");
        Button deleteButton = new Button("Delete");
        Button exitButton = new Button("Exit");
        Button exportButton = new Button("Export");

        editButton.setDisable(true);
        deleteButton.setDisable(true);

        togglePasswordButton.setOnAction(e -> {
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
        });

        generatePasswordButton.setOnAction(e -> {
            String generated = generateStrongPassword(12);
            passwordField.setText(generated);
            visiblePasswordField.setText(generated);
        });

        addButton.setOnAction(e -> {
            String platform = platformField.getText();
            String login = loginField.getText();
            String password = passwordField.isVisible() ? passwordField.getText() : visiblePasswordField.getText();

            if (!platform.isEmpty() && !login.isEmpty() && !password.isEmpty()) {

                String encryptedPassword = CryptoUtils.encrypt(password, masterPassword);

                if (editingEntry[0] != null) {
                    editingEntry[0].setPlatform(platform);
                    editingEntry[0].setLogin(login);
                    editingEntry[0].setPassword(encryptedPassword);
                    editingEntry[0] = null;
                    addButton.setText("Add");
                } else {
                    passwordManager.addEntry(new AccountEntry(platform, login, encryptedPassword));
                }

                passwordManager.saveToFile();
                refreshList(listView);

                platformField.clear();
                loginField.clear();
                passwordField.clear();
                visiblePasswordField.clear();
                editButton.setDisable(true);
                deleteButton.setDisable(true);
            }
        });

        editButton.setOnAction(e -> {
            if (selectedEntry[0] != null) {
                AccountEntry entry = selectedEntry[0];
                platformField.setText(entry.getPlatform());
                loginField.setText(entry.getLogin());
                String decryptedPassword = CryptoUtils.decrypt(entry.getPassword(), masterPassword);
                passwordField.setText(decryptedPassword);
                visiblePasswordField.setText(decryptedPassword);
                editingEntry[0] = entry;
                addButton.setText("Save");
            }
        });

        deleteButton.setOnAction(e -> {
            if (selectedEntry[0] != null) {
                passwordManager.removeEntry(selectedEntry[0]);
                passwordManager.saveToFile();
                refreshList(listView);
                editButton.setDisable(true);
                deleteButton.setDisable(true);
            }
        });

        exitButton.setOnAction(e -> stage.close());

        exportButton.setOnAction(e -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>("Plaintext CSV", "Plaintext CSV", "Encrypted JSON");
            dialog.setTitle("Export Format");
            dialog.setHeaderText("Choose export format");

            dialog.showAndWait().ifPresent(choice -> {
                if (choice.equals("Plaintext CSV")) exportToCSV();
                else exportToEncryptedJSON();
            });
        });

        refreshList(listView);

        listView.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            int index = newVal.intValue();
            List<AccountEntry> entries = passwordManager.getAllEntries();

            if (index >= 0 && index < entries.size()) {
                selectedEntry[0] = entries.get(index);
                editButton.setDisable(false);
                deleteButton.setDisable(false);
            }
        });

        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {

                int index = listView.getSelectionModel().getSelectedIndex();
                List<AccountEntry> entries = passwordManager.getAllEntries();

                if (index >= 0 && index < entries.size()) {

                    AccountEntry entry = entries.get(index);

                    Dialog<Void> dialog = new Dialog<>();
                    dialog.setTitle("Account Details");
                    dialog.setHeaderText(entry.getPlatform());

                    Label loginLabel = new Label("Login: " + entry.getLogin());

                    PasswordField hidden = new PasswordField();
                    TextField visible = new TextField();

                    String decrypted = CryptoUtils.decrypt(entry.getPassword(), masterPassword);

                    hidden.setText(decrypted);
                    visible.setText(decrypted);

                    hidden.setEditable(false);
                    visible.setEditable(false);

                    visible.setVisible(false);
                    visible.setManaged(false);

                    Button toggle = new Button("Show");
                    toggle.setOnAction(e -> {
                        if (toggle.getText().equals("Show")) {
                            visible.setVisible(true);
                            visible.setManaged(true);
                            hidden.setVisible(false);
                            hidden.setManaged(false);
                            toggle.setText("Hide");
                        } else {
                            hidden.setVisible(true);
                            hidden.setManaged(true);
                            visible.setVisible(false);
                            visible.setManaged(false);
                            toggle.setText("Show");
                        }
                    });

                    Button copy = new Button("Copy password");
                    copy.setOnAction(e -> {
                        Clipboard clipboard = Clipboard.getSystemClipboard();
                        ClipboardContent content = new ClipboardContent();
                        content.putString(decrypted);
                        clipboard.setContent(content);
                        new Alert(Alert.AlertType.INFORMATION, "Password copied!").showAndWait();
                    });

                    HBox box = new HBox(10, hidden, visible, toggle);
                    VBox content = new VBox(10, loginLabel, box, copy);

                    dialog.getDialogPane().setContent(content);
                    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                    dialog.showAndWait();
                }
            }
        });

        HBox passwordBox = new HBox(10);

        HBox.setHgrow(passwordField, Priority.ALWAYS);
        HBox.setHgrow(visiblePasswordField, Priority.ALWAYS);

        passwordField.setMaxWidth(Double.MAX_VALUE);
        visiblePasswordField.setMaxWidth(Double.MAX_VALUE);

        passwordBox.getChildren().addAll(passwordField, visiblePasswordField, togglePasswordButton);


        HBox leftButtons = new HBox(10, addButton, editButton, deleteButton, generatePasswordButton, exportButton);
        HBox rightButtons = new HBox(exitButton);
        HBox fullRow = new HBox(10, leftButtons, rightButtons);
        HBox.setHgrow(rightButtons, Priority.ALWAYS);

        layout.getChildren().addAll(
                new Label("Saved Accounts:"),
                listView,
                new Label("Add New Entry:"),
                platformField,
                loginField,
                passwordBox,
                fullRow
        );

        Scene scene = new Scene(layout, 500, 500);
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                addButton.fire();
            }
        });

        stage.setScene(scene);
        stage.setTitle("Password Manager");
        stage.show();
    }

    private void exportToCSV() {
        try (FileWriter writer = new FileWriter("password_export.csv")) {
            writer.write("Platform,Login,Password\n");
            for (AccountEntry entry : passwordManager.getAllEntries()) {
                String decrypted = CryptoUtils.decrypt(entry.getPassword(), masterPassword);
                writer.write(entry.getPlatform() + "," + entry.getLogin() + "," + decrypted + "\n");
            }
            new Alert(Alert.AlertType.INFORMATION, "Exported to password_export.csv").showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Export failed").showAndWait();
        }
    }

    private void exportToEncryptedJSON() {
        try (FileWriter writer = new FileWriter("password_export_encrypted.json")) {
            passwordManager.saveToFile(writer);
            new Alert(Alert.AlertType.INFORMATION, "Encrypted export completed").showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Export failed").showAndWait();
        }
    }

    private void refreshList(ListView<String> listView) {
        listView.getItems().clear();
        for (AccountEntry entry : passwordManager.getAllEntries()) {
            listView.getItems().add(entry.getPlatform() + " - " + entry.getLogin());
        }
    }

    private String generateStrongPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*-_=+";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    private boolean isLoginBlocked() {
        if (failedAttempts < 3 || lastFailedAttempt == null) return false;
        long seconds = Duration.between(lastFailedAttempt, Instant.now()).getSeconds();
        return seconds < getSecondsToWait();
    }

    private long getSecondsToWait() {
        return switch (failedAttempts) {
            case 3 -> 30;
            case 4 -> 60;
            case 5 -> 180;
            case 6 -> 300;
            case 7 -> 1200;
            case 8 -> 3600;
            default -> 86400;
        };
    }

    private String formatWaitTime(long seconds) {
        if (seconds >= 3600) return (seconds / 3600) + " hour(s)";
        if (seconds >= 60) return (seconds / 60) + " minute(s)";
        return seconds + " second(s)";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
