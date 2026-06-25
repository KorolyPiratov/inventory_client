package inventory_client.controller;

import inventory_client.MainApp;
import inventory_client.service.ApiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ServerSetupController {

    @FXML private TextField ipField;
    @FXML private Label errorLabel;

    @FXML
    private void handleConnect() {
        String ip = ipField.getText().trim();
        if (ip.isBlank()) {
            errorLabel.setText("Введите IP-адрес");
            return;
        }

        // Проверяем соединение
        errorLabel.setText("Проверка соединения...");
        new Thread(() -> {
            try {
                ApiService.getInstance().setServerIp(ip);
                // Пробуем достучаться до сервера
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://" + ip + ":8080/api/auth/login"))
                        .timeout(java.time.Duration.ofSeconds(5))
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}"))
                        .header("Content-Type", "application/json")
                        .build();
                client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

                // Любой ответ = сервер живой
                javafx.application.Platform.runLater(() -> {
                    try {
                        MainApp.showLoginScreen();
                    } catch (Exception e) {
                        errorLabel.setText("Ошибка перехода: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        errorLabel.setText("Не удалось подключиться. Проверьте IP.")
                );
            }
        }).start();
    }
}