package inventory_client.controller;

import inventory_client.MainApp;
import inventory_client.service.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @FXML
    private void handleChangeServer(MouseEvent event) {
        String current = ApiService.getInstance().getServerIp();
        TextInputDialog dialog = new TextInputDialog(current);
        dialog.setTitle("Сменить сервер");
        dialog.setHeaderText("Введите IP-адрес сервера");
        dialog.setContentText("IP:");
        dialog.showAndWait().ifPresent(ip -> {
            if (!ip.isBlank()) {
                ApiService.getInstance().setServerIp(ip.trim());
                errorLabel.setText("Сервер изменён: " + ip.trim());
            }
        });
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Введите логин и пароль");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setText("");

        new Thread(() -> {
            try {
                ApiService.getInstance().login(username, password);
                Platform.runLater(() -> {
                    try {
                        MainApp.showMainScreen();
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorLabel.setText("Ошибка открытия главного экрана");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorLabel.setText("Неверный логин или пароль");
                    loginButton.setDisable(false);
                });
            }
        }).start();

    }
}