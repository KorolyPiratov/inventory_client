package inventory_client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import inventory_client.service.ApiService;

public class MainApp extends Application {

	private static Stage primaryStage;

	@Override
	public void start(Stage stage) throws Exception {
		primaryStage = stage;
		primaryStage.setTitle("Система учёта");
		primaryStage.getIcons().add(
				new javafx.scene.image.Image(
						getClass().getResourceAsStream("/icon.png")));
		if (!ApiService.getInstance().hasServerIp()) {
			showServerSetupScreen();
		} else {
			showLoginScreen();
		}

		primaryStage.show();
	}

	public static void showServerSetupScreen() throws Exception {
		FXMLLoader loader = new FXMLLoader(
				MainApp.class.getResource("/fxml/server_setup.fxml"));
		Scene scene = new Scene(loader.load(), 400, 250);
		primaryStage.setScene(scene);
	}

	public static void showLoginScreen() throws Exception {
		FXMLLoader loader = new FXMLLoader(
				MainApp.class.getResource("/fxml/login.fxml"));
		Scene scene = new Scene(loader.load(), 400, 300);
		primaryStage.setScene(scene);
	}

	public static void showMainScreen() throws Exception {
		FXMLLoader loader = new FXMLLoader(
				MainApp.class.getResource("/fxml/main.fxml"));
		Scene scene = new Scene(loader.load(), 1100, 650);
		primaryStage.setScene(scene);
	}

	public static Stage getPrimaryStage() {
		return primaryStage;
	}

	public static void main(String[] args) {
		launch(args);
	}
}