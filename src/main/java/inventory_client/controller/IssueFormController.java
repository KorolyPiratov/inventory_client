package inventory_client.controller;

import inventory_client.model.Item;
import inventory_client.service.ApiService;
import inventory_client.service.PrintService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.LocalDate;

public class IssueFormController {

    @FXML private Label titleLabel;
    @FXML private Label dateLabel;
    @FXML private TextField fullNameField;
    @FXML private CheckBox indefiniteCheck;
    @FXML private CheckBox printCheck;
    @FXML private Label errorLabel;
    @FXML private Label templateNameLabel;
    @FXML private VBox returnDateBox;
    @FXML private DatePicker returnDatePicker;

    private Item item;

    public void setItem(Item item) {
        this.item = item;
        titleLabel.setText("Выдача: " + item.getName());
        dateLabel.setText(LocalDate.now().toString());
    }

    @FXML
    private void handleIndefiniteChanged() {
        boolean indefinite = indefiniteCheck.isSelected();
        returnDateBox.setVisible(!indefinite);
        returnDateBox.setManaged(!indefinite);
        if (indefinite) returnDatePicker.setValue(null);
    }

    @FXML
    private void handleChooseTemplate() {
        Stage stage = (Stage) fullNameField.getScene().getWindow();
        PrintService.chooseTemplate(stage);
        if (PrintService.getCustomTemplatePath() != null) {
            templateNameLabel.setText(
                    PrintService.getCustomTemplatePath().getFileName().toString());
            templateNameLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11;");
        }
    }

    @FXML
    private void handleConfirm() {
        String fullName = fullNameField.getText().trim();
        if (fullName.isEmpty()) {
            errorLabel.setText("Введите ФИО получателя");
            return;
        }

        if (item.getQuantity() <= 0) {
            errorLabel.setText("Вещь закончилась на складе!");
            return;
        }

        boolean indefinite = indefiniteCheck.isSelected();
        LocalDate returnDate = indefinite ? null : returnDatePicker.getValue();

        if (!indefinite && returnDate == null) {
            errorLabel.setText("Укажите дату возврата или выберите бессрочно");
            return;
        }

        try {
            ApiService.getInstance().issueItem(
                    item.getId(), fullName, indefinite, returnDate);

            if (printCheck.isSelected()) {
                PrintService.printIssuance(item, fullName,
                        LocalDate.now().toString(), indefinite);
            }

            closeWindow();
        } catch (Exception e) {
            errorLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    @FXML private void handleCancel() { closeWindow(); }

    private void closeWindow() {
        ((Stage) fullNameField.getScene().getWindow()).close();
    }
}