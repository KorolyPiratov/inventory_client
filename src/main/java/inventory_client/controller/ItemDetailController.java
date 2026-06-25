package inventory_client.controller;

import inventory_client.model.Issuance;
import inventory_client.model.Item;
import inventory_client.service.ApiService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.List;

public class ItemDetailController {

    @FXML private Label titleLabel;
    @FXML private Label categoryLabel;
    @FXML private Label colorLabel;
    @FXML private Label manufacturerLabel;
    @FXML private Label boxLabel;
    @FXML private Label quantityLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<Issuance> issuanceTable;
    @FXML private TableColumn<Issuance, String> fullNameCol;
    @FXML private TableColumn<Issuance, String> dateCol;
    @FXML private TableColumn<Issuance, Boolean> indefiniteCol;
    @FXML private Label printerLabel;

    private Item item;

    public void setItem(Item item) {
        this.item = item;
        fillData();
        loadIssuances();
    }

    private void fillData() {
        titleLabel.setText(item.getName());
        categoryLabel.setText(item.getCategory() != null ? item.getCategory() : "—");
        colorLabel.setText(item.getColorType() != null ? item.getColorType() : "—");
        manufacturerLabel.setText(item.getSupplyDate() != null ? item.getSupplyDate().toString() : "—");
        boxLabel.setText(item.getBoxNumber() != null ? item.getBoxNumber() : "—");
        quantityLabel.setText(String.valueOf(item.getQuantity()));
        printerLabel.setText(item.getPrinterName() != null ? item.getPrinterName() : "—");
        descriptionLabel.setText(item.getDescription() != null ? item.getDescription() : "—");

        fullNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("issuedAt"));
        indefiniteCol.setCellValueFactory(new PropertyValueFactory<>("isIndefinite"));
    }

    private void loadIssuances() {
        new Thread(() -> {
            try {
                List<Issuance> issuances = ApiService.getInstance()
                        .getIssuances(item.getId());
                Platform.runLater(() ->
                        issuanceTable.setItems(
                                FXCollections.observableArrayList(issuances)));
            } catch (Exception e) {
                Platform.runLater(() ->
                        statusLabel.setText("Ошибка загрузки выдач"));
            }
        }).start();
    }

    @FXML
    private void handleIssue() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/issue_form.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Выдать: " + item.getName());
            stage.setScene(new Scene(loader.load(), 420, 400));
            stage.initModality(Modality.APPLICATION_MODAL);

            IssueFormController controller = loader.getController();
            controller.setItem(item);

            stage.showAndWait();
            loadIssuances();

            // Обновляем количество
            Item updated = ApiService.getInstance()
                    .getItems()
                    .stream()
                    .filter(i -> i.getId().equals(item.getId()))
                    .findFirst()
                    .orElse(item);
            item.setQuantity(updated.getQuantity());
            quantityLabel.setText(String.valueOf(item.getQuantity()));

        } catch (Exception e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    private void handleEdit() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/item_form.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Редактировать: " + item.getName());
            stage.setScene(new Scene(loader.load(), 450, 500));
            stage.initModality(Modality.APPLICATION_MODAL);

            ItemFormController controller = loader.getController();
            controller.setItem(item);

            stage.showAndWait();
        } catch (Exception e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
        }
    }
    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удаление");
        confirm.setHeaderText("Удалить \"" + item.getName() + "\"?");
        confirm.setContentText("Вещь и вся история её выдач будут удалены безвозвратно.");
        ButtonType yes = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
        ButtonType no  = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);

        confirm.showAndWait().ifPresent(r -> {
            if (r != yes) return;
            new Thread(() -> {
                try {
                    ApiService.getInstance().deleteItem(item.getId());
                    Platform.runLater(() ->
                            ((Stage) titleLabel.getScene().getWindow()).close());
                } catch (Exception e) {
                    Platform.runLater(() ->
                            statusLabel.setText("Ошибка удаления: " + e.getMessage()));
                }
            }).start();
        });
    }
}