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

        Scene loginScene = new Scene(loginLayout, 300, 180);
        stage.setScene(loginScene);
        stage.setTitle("Password Manager - Login");
        stage.show();

        if (!masterPasswordManager.isPasswordSet()) {
            Label label = new Label("Set master password:");
            PasswordField newPasswordField = new PasswordField();
            newPasswordField.setPromptText("Password");
            Button setButton = new Button("Set Password");

            loginLayout.getChildren().addAll(label, newPasswordField, setButton);

            setButton.setOnAction(e -> {
                String newPassword = newPasswordField.getText();
                if (!newPassword.isEmpty()) {
                    masterPasswordManager.setPassword(newPassword);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Password set. Please log in.", ButtonType.OK);
                    alert.showAndWait();
                    showLoginScreen(stage);
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
            passwordField.setPromptText("Password");
            Button loginButton = new Button("Login");

            loginLayout.getChildren().addAll(label, passwordField, loginButton);

            loginButton.setOnAction(e -> {
                if (isLoginBlocked()) {
                    long remaining = getSecondsToWait() - Duration.between(lastFailedAttempt, Instant.now()).getSeconds();
                    if (remaining < 0) remaining = 0;

                    Alert alert = new Alert(Alert.AlertType.WARNING,
                            "Too many failed attempts.\nPlease wait " + formatWaitTime(remaining) + ".", ButtonType.OK);
                    alert.showAndWait();
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

                    if (failedAttempts >= 3) {
                        long waitTime = getSecondsToWait();
                        Alert alert = new Alert(Alert.AlertType.WARNING,
                                "Too many failed attempts.\nPlease wait " + formatWaitTime(waitTime) + ".", ButtonType.OK);
                        alert.showAndWait();
                    } else {
                        int remainingAttempts = 3 - failedAttempts;
                        Alert alert = new Alert(Alert.AlertType.ERROR,
                                "Invalid master password.\nAttempts left: " + remainingAttempts, ButtonType.OK);
                        alert.showAndWait();
                    }
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
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Confirm Deletion");
                confirmAlert.setHeaderText("Are you sure you want to delete this entry?");
                confirmAlert.setContentText(selectedEntry[0].getPlatform() + " - " + selectedEntry[0].getLogin());

                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        passwordManager.removeEntry(selectedEntry[0]);
                        passwordManager.saveToFile();
                        refreshList(listView);
                        editButton.setDisable(true);
                        deleteButton.setDisable(true);
                    }
                });
            }
        });

        exitButton.setOnAction(e -> stage.close());

        refreshList(listView);

        listView.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            int index = newVal.intValue();
            List<AccountEntry> entries = passwordManager.getAllEntries();

            if (index >= 0 && index < entries.size()) {
                selectedEntry[0] = entries.get(index);
                editButton.setDisable(false);
                deleteButton.setDisable(false);
            } else {
                selectedEntry[0] = null;
                editButton.setDisable(true);
                deleteButton.setDisable(true);
            }
        });

        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int selectedIndex = listView.getSelectionModel().getSelectedIndex();
                List<AccountEntry> entries = passwordManager.getAllEntries();

                if (selectedIndex >= 0 && selectedIndex < entries.size()) {
                    AccountEntry clickedEntry = entries.get(selectedIndex);

                    Dialog<Void> dialog = new Dialog<>();
                    dialog.setTitle("Account Details");
                    dialog.setHeaderText(clickedEntry.getPlatform());

                    Label loginLabel = new Label("Login: " + clickedEntry.getLogin());

                    PasswordField hiddenPasswordField = new PasswordField();
                    TextField visiblePasswordFieldDialog = new TextField();

                    String decryptedPassword = CryptoUtils.decrypt(clickedEntry.getPassword(), masterPassword);
                    hiddenPasswordField.setText(decryptedPassword);
                    visiblePasswordFieldDialog.setText(decryptedPassword);

                    hiddenPasswordField.setEditable(false);
                    visiblePasswordFieldDialog.setEditable(false);
                    visiblePasswordFieldDialog.setVisible(false);
                    visiblePasswordFieldDialog.setManaged(false);

                    Button toggleButton = new Button("Pokaż");

                    toggleButton.setOnAction(e -> {
                        if (toggleButton.getText().equals("Pokaż")) {
                            visiblePasswordFieldDialog.setVisible(true);
                            visiblePasswordFieldDialog.setManaged(true);
                            hiddenPasswordField.setVisible(false);
                            hiddenPasswordField.setManaged(false);
                            toggleButton.setText("Ukryj");
                        } else {
                            hiddenPasswordField.setVisible(true);
                            hiddenPasswordField.setManaged(true);
                            visiblePasswordFieldDialog.setVisible(false);
                            visiblePasswordFieldDialog.setManaged(false);
                            toggleButton.setText("Pokaż");
                        }
                    });

                    Button copyButton = new Button("Skopiuj hasło");
                    copyButton.setOnAction(e -> {
                        Clipboard clipboard = Clipboard.getSystemClipboard();
                        ClipboardContent content = new ClipboardContent();
                        content.putString(decryptedPassword);
                        clipboard.setContent(content);

                        Alert copiedAlert = new Alert(Alert.AlertType.INFORMATION, "Hasło skopiowane!", ButtonType.OK);
                        copiedAlert.showAndWait();
                    });

                    HBox passwordBox = new HBox(10, hiddenPasswordField, visiblePasswordFieldDialog, toggleButton);
                    VBox content = new VBox(10, loginLabel, passwordBox, copyButton);
                    content.setStyle("-fx-padding: 10;");

                    dialog.getDialogPane().setContent(content);
                    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

                    dialog.showAndWait();
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

        HBox leftButtonBox = new HBox(10, addButton, editButton, deleteButton, generatePasswordButton);
        HBox rightButtonBox = new HBox(exitButton);
        rightButtonBox.setStyle("-fx-alignment: center-right;");

        HBox fullButtonRow = new HBox(10);
        fullButtonRow.getChildren().addAll(leftButtonBox, rightButtonBox);
        HBox.setHgrow(rightButtonBox, Priority.ALWAYS);

        layout.getChildren().addAll(
                new Label("Saved Accounts:"),
                listView,
                new Label("Add New Entry:"),
                platformField,
                loginField,
                passwordBox,
                fullButtonRow
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

    private void refreshList(ListView<String> listView) {
        listView.getItems().clear();
        List<AccountEntry> entries = passwordManager.getAllEntries();

        for (AccountEntry entry : entries) {
            String itemText = entry.getPlatform() + " - " + entry.getLogin();
            listView.getItems().add(itemText);
        }
    }

    private String generateStrongPassword(int length) {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String symbols = "!@#$%^&*-_=+";
        String all = upper + lower + digits + symbols;

        StringBuilder password = new StringBuilder();
        password.append(upper.charAt((int) (Math.random() * upper.length())));
        password.append(lower.charAt((int) (Math.random() * lower.length())));
        password.append(digits.charAt((int) (Math.random() * digits.length())));
        password.append(symbols.charAt((int) (Math.random() * symbols.length())));

        for (int i = 4; i < length; i++) {
            password.append(all.charAt((int) (Math.random() * all.length())));
        }

        return shuffleString(password.toString());
    }

    private String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int randomIndex = (int) (Math.random() * chars.length);
            char temp = chars[i];
            chars[i] = chars[randomIndex];
            chars[randomIndex] = temp;
        }
        return new String(chars);
    }

    private boolean isLoginBlocked() {
        if (failedAttempts < 3 || lastFailedAttempt == null) return false;

        long secondsSinceLastAttempt = Duration.between(lastFailedAttempt, Instant.now()).getSeconds();
        long requiredWait = getSecondsToWait();

        return secondsSinceLastAttempt < requiredWait;
    }

    private long getSecondsToWait() {
        return switch (failedAttempts) {
            case 3 -> 30;       // 30 sekund
            case 4 -> 60;       // 1 minuta
            case 5 -> 180;      // 3 minuty
            case 6 -> 300;      // 5 minut
            case 7 -> 1200;     // 20 minut
            case 8 -> 3600;     // 1 godzina
            default -> 86400;   // 1 dzień
        };
    }

    private String formatWaitTime(long seconds) {
        if (seconds >= 86400) return "1 day";
        if (seconds >= 3600) return (seconds / 3600) + " hour(s)";
        if (seconds >= 60) return (seconds / 60) + " minute(s)";
        return seconds + " second(s)";
    }


    public static void main(String[] args) {
        launch(args);
    }
}
