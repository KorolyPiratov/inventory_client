package inventory_client.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsController {

    @FXML private ListView<String> optionsList;
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private Button actionButton;

    private final Map<String, String> descriptions = new LinkedHashMap<>();
    private final Map<String, Runnable> actions = new LinkedHashMap<>();

    private Stage stage;

    @FXML
    public void initialize() {
        optionsList.setItems(FXCollections.observableArrayList());

        optionsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            showOption(newVal);
        });
    }

    public void addOption(String title, String description, Runnable action) {
        descriptions.put(title, description);
        actions.put(title, action);
        optionsList.getItems().add(title);
    }

    public void selectFirst() {
        if (!optionsList.getItems().isEmpty()) {
            optionsList.getSelectionModel().select(0);
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void showOption(String title) {
        titleLabel.setText(title);
        descriptionLabel.setText(descriptions.getOrDefault(title, ""));

        actionButton.setOnAction(e -> {
            Runnable action = actions.get(title);
            if (action != null) action.run();
        });
    }
}
