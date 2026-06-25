package inventory_client.controller;

import inventory_client.model.Item;
import inventory_client.service.ApiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;

public class ItemFormController {

    @FXML private Label titleLabel;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryField;
    @FXML private TextField customCategoryField;
    @FXML private ComboBox<String> colorField;
    @FXML private TextField boxField;
    @FXML private TextField quantityField;
    @FXML private DatePicker supplyDateField;
    @FXML private TextArea descriptionField;
    @FXML private Label errorLabel;
    @FXML private TextField printerField;

    private Item item;

    private final ObservableList<String> categories = FXCollections.observableArrayList(
            "Картридж", "Устройство", "Другое");

    @FXML
    public void initialize() {
        categoryField.setItems(categories);
        colorField.setItems(FXCollections.observableArrayList(
                "Жёлтый", "Циан", "Пурпурный", "Чёрный"));
        titleLabel.setText("Добавить вещь");
    }

    @FXML
    private void handleCategoryChanged() {
        boolean isOther = "Другое".equals(categoryField.getValue());
        customCategoryField.setVisible(isOther);
        customCategoryField.setManaged(isOther);
        if (!isOther) customCategoryField.clear();
    }

    public void setItem(Item item) {
        this.item = item;
        titleLabel.setText("Редактировать: " + item.getName());
        nameField.setText(item.getName());
        colorField.setValue(item.getColorType());
        boxField.setText(item.getBoxNumber() != null ? item.getBoxNumber() : "");
        quantityField.setText(String.valueOf(item.getQuantity()));
        descriptionField.setText(item.getDescription() != null ? item.getDescription() : "");
        supplyDateField.setValue(item.getSupplyDate());
        printerField.setText(item.getPrinterName() != null ? item.getPrinterName() : "");

        String cat = item.getCategory();
        if (cat != null && !cat.isEmpty()) {
            if (!categories.contains(cat)) categories.add(0, cat);
            categoryField.setValue(cat);
        }
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) { errorLabel.setText("Введите название"); return; }

        String category = categoryField.getValue();
        if ("Другое".equals(category)) {
            String custom = customCategoryField.getText().trim();
            if (custom.isEmpty()) { errorLabel.setText("Введите свою категорию"); return; }
            category = custom;
            if (!categories.contains(custom)) categories.add(categories.size() - 1, custom);
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
        } catch (NumberFormatException e) {
            errorLabel.setText("Количество должно быть числом");
            return;
        }

        Item toSave = item != null ? item : new Item();
        toSave.setName(name);
        toSave.setCategory(category);
        toSave.setColorType(colorField.getValue());
        toSave.setBoxNumber(boxField.getText().trim());
        toSave.setQuantity(quantity);
        toSave.setSupplyDate(supplyDateField.getValue());
        toSave.setPrinterName(printerField.getText().trim());
        toSave.setDescription(descriptionField.getText().trim());

        final String finalCategory = category;
        new Thread(() -> {
            try {
                if (item != null && item.getId() != null) {
                    ApiService.getInstance().updateItem(item.getId(), toSave);
                } else {
                    ApiService.getInstance().createItem(toSave);
                }
                javafx.application.Platform.runLater(() -> {
                    if (!categories.contains(finalCategory))
                        categories.add(categories.size() - 1, finalCategory);
                    closeWindow();
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        errorLabel.setText("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleCancel() { closeWindow(); }

    private void closeWindow() {
        ((Stage) nameField.getScene().getWindow()).close();
    }
}