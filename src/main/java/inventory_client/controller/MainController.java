package inventory_client.controller;

import inventory_client.MainApp;
import inventory_client.model.Issuance;
import inventory_client.model.Item;
import inventory_client.service.ApiService;
import inventory_client.service.ExcelService;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class MainController {

    @FXML private TableView<Item> itemsTable;
    @FXML private TableColumn<Item, String>    nameCol;
    @FXML private TableColumn<Item, String>    categoryCol;
    @FXML private TableColumn<Item, String>    colorCol;
    @FXML private TableColumn<Item, String>    boxCol;
    @FXML private TableColumn<Item, LocalDate> supplyDateCol;
    @FXML private TableColumn<Item, Integer>   quantityCol;
    @FXML private TableColumn<Item, String>    printerCol;

    @FXML private TableView<Issuance> archiveTable;
    @FXML private TableColumn<Issuance, String>  archiveItemCol;
    @FXML private TableColumn<Issuance, String>  archivePersonCol;
    @FXML private TableColumn<Issuance, String>  archiveDateCol;
    @FXML private TableColumn<Issuance, Boolean> archiveIndefiniteCol;
    @FXML private TableColumn<Issuance, String>  archiveReturnDateCol;
    @FXML private TableColumn<Issuance, String>  archivePrinterCol;

    @FXML private TextField        searchField;
    @FXML private TextField        archiveSearchField;
    @FXML private VBox             archiveSearchBox;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> colorFilter;
    @FXML private TextField        boxFilter;
    @FXML private Label            statusLabel;
    @FXML private TabPane          tabPane;
    @FXML private VBox             filtersPanel;
    @FXML private HBox             topSearchBox;

    private List<Item>     allItems     = List.of();
    private List<Issuance> allIssuances = List.of();

    // ===== INIT =====

    @FXML
    public void initialize() {
        // Склад — лямбда-фабрики чтобы компаратор работал корректно
        nameCol.setCellValueFactory(      d -> new SimpleStringProperty(d.getValue().getName()));
        categoryCol.setCellValueFactory(  d -> new SimpleStringProperty(d.getValue().getCategory()));
        colorCol.setCellValueFactory(     d -> new SimpleStringProperty(d.getValue().getColorType()));
        boxCol.setCellValueFactory(       d -> new SimpleStringProperty(d.getValue().getBoxNumber()));
        printerCol.setCellValueFactory(   d -> new SimpleStringProperty(d.getValue().getPrinterName()));
        supplyDateCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getSupplyDate()));
        quantityCol.setCellValueFactory(  d -> new SimpleObjectProperty<>(d.getValue().getQuantity()));

        // Компаратор: null и "" — всегда вниз
        Comparator<String> strNullsLast = (a, b) -> {
            boolean aEmpty = a == null || a.isEmpty();
            boolean bEmpty = b == null || b.isEmpty();
            if (aEmpty && bEmpty) return 0;
            if (aEmpty) return 1;
            if (bEmpty) return -1;
            return a.compareToIgnoreCase(b);
        };
        Comparator<LocalDate> dateNullsLast = (a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            return a.compareTo(b);
        };
        Comparator<Integer> intNullsLast = (a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            return Integer.compare(a, b);
        };


        nameCol.setComparator(nullsLastString(nameCol));
        categoryCol.setComparator(nullsLastString(categoryCol));
        colorCol.setComparator(nullsLastString(colorCol));
        boxCol.setComparator(nullsLastString(boxCol));
        printerCol.setComparator(nullsLastString(printerCol));
        supplyDateCol.setComparator(nullsLastDate(supplyDateCol));
        quantityCol.setComparator(nullsLastComparable(quantityCol));



        // Подсветка строк с quantity <= 0
        itemsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) setStyle("");
                else if (item.getQuantity() != null && item.getQuantity() <= 0)
                    setStyle("-fx-background-color: #fadbd8;");
                else setStyle("");
            }
        });

        // Архив
        archiveItemCol.setCellValueFactory(     d -> new SimpleStringProperty(d.getValue().getItemName()));
        archivePersonCol.setCellValueFactory(   d -> new SimpleStringProperty(d.getValue().getFullName()));
        archiveDateCol.setCellValueFactory(     d -> new SimpleObjectProperty<>(d.getValue().getIssuedAt()));
        archiveIndefiniteCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getIsIndefinite()));
        archiveReturnDateCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getReturnDate()));
        archivePrinterCol.setCellValueFactory(  d -> new SimpleStringProperty(d.getValue().getPrinterName()));

        ContextMenu archiveContextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Удалить запись");
        archiveContextMenu.getItems().add(deleteItem);

        archiveTable.setRowFactory(tv -> {
            TableRow<Issuance> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    deleteItem.setOnAction(e -> handleDeleteIssuance(row.getItem()));
                    archiveContextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            row.setOnMousePressed(event -> {
                if (!event.isSecondaryButtonDown()) archiveContextMenu.hide();
            });
            return row;
        });

        // Цвета
        colorFilter.getItems().setAll("Все цвета", "Жёлтый", "Циан", "Пурпурный", "Чёрный");
        colorFilter.setValue("Все цвета");

        Platform.runLater(this::loadItems);
    }

    // ===== ТАБЫ =====

    @FXML
    private void handleTabChanged() {
        boolean isArchive = tabPane.getSelectionModel().getSelectedIndex() == 1;
        filtersPanel.setVisible(!isArchive);
        filtersPanel.setManaged(!isArchive);
        topSearchBox.setVisible(!isArchive);
        topSearchBox.setManaged(!isArchive);
        archiveSearchBox.setVisible(isArchive);
        archiveSearchBox.setManaged(isArchive);
    }

    @FXML
    private void handleArchiveTabSelected() {
        handleTabChanged();
        loadArchive();
    }

    // ===== ЗАГРУЗКА =====

    @FXML
    private void handleRefresh() {
        loadItems();
        if (tabPane.getSelectionModel().getSelectedIndex() == 1) loadArchive();
    }

    private void loadItems() {
        statusLabel.setText("Загрузка...");
        new Thread(() -> {
            try {
                List<Item> items = ApiService.getInstance().getItems();
                Platform.runLater(() -> {
                    allItems = items;
                    updateCategoryFilter();
                    applyFilters();
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Ошибка загрузки"));
            }
        }).start();
    }

    private void loadArchive() {
        new Thread(() -> {
            try {
                List<Issuance> issuances = ApiService.getInstance().getAllIssuances();
                Platform.runLater(() -> {
                    allIssuances = issuances;
                    applyArchiveSearch();
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Ошибка загрузки архива"));
            }
        }).start();
    }

    // ===== ФИЛЬТРЫ =====

    private void updateCategoryFilter() {
        String current = categoryFilter.getValue();
        List<String> cats = allItems.stream()
                .map(Item::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .distinct().sorted().collect(Collectors.toList());

        categoryFilter.getItems().clear();
        categoryFilter.getItems().add("Все категории");
        categoryFilter.getItems().addAll(cats);

        categoryFilter.setValue(
                categoryFilter.getItems().contains(current) ? current : "Все категории");
    }

    @FXML private void handleFilterChanged() { applyFilters(); }
    @FXML private void handleSearchTyped()   { applyFilters(); }
    @FXML private void handleSearch()        { applyFilters(); }

    private void applyFilters() {
        String colorType = colorFilter.getValue();
        String category  = categoryFilter.getValue();
        String boxNumber = boxFilter.getText().trim();
        String search    = searchField.getText().trim().toLowerCase();

        List<Item> filtered = allItems.stream().filter(item -> {
            if (category != null && !"Все категории".equals(category)
                    && !category.equals(item.getCategory())) return false;
            if (colorType != null && !"Все цвета".equals(colorType)
                    && !colorType.equals(item.getColorType())) return false;
            if (!boxNumber.isEmpty() && (item.getBoxNumber() == null
                    || !item.getBoxNumber().toLowerCase().contains(boxNumber.toLowerCase())))
                return false;
            if (!search.isEmpty()) {
                boolean matchName    = item.getName() != null
                        && item.getName().toLowerCase().contains(search);
                boolean matchPrinter = item.getPrinterName() != null
                        && item.getPrinterName().toLowerCase().contains(search);
                if (!matchName && !matchPrinter) return false;
            }
            return true;
        }).collect(Collectors.toList());

        itemsTable.setItems(FXCollections.observableArrayList(filtered));
        statusLabel.setText("Показано: " + filtered.size() + " из " + allItems.size());
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        categoryFilter.setValue("Все категории");
        colorFilter.setValue("Все цвета");
        boxFilter.clear();
        updateCategoryFilter();
        applyFilters();
    }

    // ===== ПОИСК ПО АРХИВУ =====

    @FXML private void handleArchiveSearch() { applyArchiveSearch(); }

    private void applyArchiveSearch() {
        String query = archiveSearchField != null
                ? archiveSearchField.getText().trim().toLowerCase() : "";

        List<Issuance> filtered = allIssuances.stream().filter(iss -> {
            if (query.isEmpty()) return true;
            boolean matchItem   = iss.getItemName() != null
                    && iss.getItemName().toLowerCase().contains(query);
            boolean matchPerson = iss.getFullName() != null
                    && iss.getFullName().toLowerCase().contains(query);
            return matchItem || matchPerson;
        }).collect(Collectors.toList());

        archiveTable.setItems(FXCollections.observableArrayList(filtered));
    }

    // ===== НАСТРОЙКИ =====

    @FXML
    private void handleSettings() {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Настройки");

        ButtonType importBtn        = new ButtonType("Импорт склада");
        ButtonType exportItemsBtn   = new ButtonType("Экспорт склада");
        ButtonType exportArchiveBtn = new ButtonType("Экспорт архива");
        ButtonType changeIpBtn      = new ButtonType("Сменить сервер");
        ButtonType deleteAllBtn     = new ButtonType("Очистить склад");
        ButtonType cleanArchiveBtn = new ButtonType(" Очистить архив");
        ButtonType backupsBtn = new ButtonType("Корзина");
        ButtonType closeBtn         = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(
                importBtn, exportItemsBtn, exportArchiveBtn,
                changeIpBtn, deleteAllBtn, cleanArchiveBtn, backupsBtn, closeBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty()) return;

        if      (result.get() == importBtn)        handleImportItems();
        else if (result.get() == exportItemsBtn)   handleExportItems();
        else if (result.get() == exportArchiveBtn) handleExportArchive();
        else if (result.get() == changeIpBtn)      handleChangeIp();
        else if (result.get() == deleteAllBtn)     handleDeleteAll();
        else if (result.get() == cleanArchiveBtn) handleCleanArchive();
        else if (result.get() == backupsBtn) handleShowBackups();
    }

    private void handleChangeIp() {
        TextInputDialog dialog = new TextInputDialog(ApiService.getInstance().getServerIp());
        dialog.setTitle("Сменить сервер");
        dialog.setHeaderText("Введите IP-адрес сервера");
        dialog.setContentText("IP:");
        dialog.showAndWait().ifPresent(ip -> {
            if (ip == null || ip.isBlank()) return;
            ApiService.getInstance().setServerIp(ip.trim());
            try { MainApp.showLoginScreen(); }
            catch (Exception e) { statusLabel.setText("Ошибка перехода: " + e.getMessage()); }
        });
    }

    private void handleDeleteAll() {
        TextInputDialog pwdDialog = new TextInputDialog();
        pwdDialog.setTitle("Подтверждение удаления");
        pwdDialog.setHeaderText("Введите пароль для очистки склада");
        pwdDialog.setContentText("Пароль:");

        Optional<String> pwdResult = pwdDialog.showAndWait();
        if (pwdResult.isEmpty()) return;

        if (!"ktgs228".equals(pwdResult.get().trim())) {
            new Alert(Alert.AlertType.ERROR, "Неверный пароль. Доступ запрещён.")
                    .showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Удалить ВЕСЬ склад?");
        confirm.setContentText("Эта операция необратима. Продолжить?");
        ButtonType yes = new ButtonType("Да, удалить всё", ButtonBar.ButtonData.OK_DONE);
        ButtonType no  = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);

        confirm.showAndWait().ifPresent(r -> {
            if (r != yes) return;
            new Thread(() -> {
                try {
                    ApiService.getInstance().deleteAllItems();
                    Platform.runLater(() -> { showAlert("Готово", "Склад очищен."); loadItems(); });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Ошибка", "Не удалось очистить: " + e.getMessage()));
                }
            }).start();
        });
    }

    // ===== EXCEL =====

    private void handleImportItems() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Импортировать склад из Excel");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel файл", "*.xlsx"));
        File file = chooser.showOpenDialog(MainApp.getPrimaryStage());
        if (file == null) return;

        new Thread(() -> {
            try {
                List<Item> imported = ExcelService.importItems(file);
                List<Item>   toCreate = new ArrayList<>();
                List<Item[]> toMerge  = new ArrayList<>();

                for (Item imp : imported) {
                    Optional<Item> existing = allItems.stream().filter(ex ->
                            eq(ex.getName(),      imp.getName())     &&
                                    eq(ex.getCategory(),  imp.getCategory()) &&
                                    eq(ex.getColorType(), imp.getColorType()) &&
                                    eq(ex.getBoxNumber(), imp.getBoxNumber())
                    ).findFirst();
                    if (existing.isPresent()) toMerge.add(new Item[]{imp, existing.get()});
                    else toCreate.add(imp);
                }

                Platform.runLater(() -> {
                    toCreate.forEach(item -> {
                        try { ApiService.getInstance().createItem(item); }
                        catch (Exception ignored) {}
                    });

                    for (Item[] pair : toMerge) {
                        Item imp = pair[0], ex = pair[1];
                        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
                        c.setTitle("Совпадение найдено");
                        c.setHeaderText("Вещь уже существует: " + ex.getName());
                        c.setContentText("Текущее: " + ex.getQuantity()
                                + "\nДобавить: " + imp.getQuantity()
                                + "\nИтого: " + (ex.getQuantity() + imp.getQuantity())
                                + "\n\nОбъединить количество?");
                        ButtonType merge = new ButtonType("Объединить");
                        ButtonType skip  = new ButtonType("Пропустить", ButtonBar.ButtonData.CANCEL_CLOSE);
                        c.getButtonTypes().setAll(merge, skip);
                        c.showAndWait().ifPresent(r -> {
                            if (r == merge) {
                                ex.setQuantity(ex.getQuantity() + imp.getQuantity());
                                try { ApiService.getInstance().updateItem(ex.getId(), ex); }
                                catch (Exception ignored) {}
                            }
                        });
                    }

                    showAlert("Импорт завершён",
                            "Создано: " + toCreate.size() + "\nСовпадений: " + toMerge.size());
                    loadItems();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка", "Импорт не удался: " + e.getMessage()));
            }
        }).start();
    }

    private void handleExportItems() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить склад как Excel");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel файл", "*.xlsx"));
        chooser.setInitialFileName("склад.xlsx");
        File file = chooser.showSaveDialog(MainApp.getPrimaryStage());
        if (file == null) return;

        new Thread(() -> {
            try {
                ExcelService.exportItems(allItems, file);
                Platform.runLater(() -> showAlert("Экспорт", "Склад экспортирован: " + file.getName()));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка", "Не удалось экспортировать: " + e.getMessage()));
            }
        }).start();
    }

    private void handleExportArchive() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить архив как Excel");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel файл", "*.xlsx"));
        chooser.setInitialFileName("архив.xlsx");
        File file = chooser.showSaveDialog(MainApp.getPrimaryStage());
        if (file == null) return;

        new Thread(() -> {
            try {
                ExcelService.exportArchive(allIssuances, file);
                Platform.runLater(() -> showAlert("Экспорт", "Архив экспортирован: " + file.getName()));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка", "Не удалось экспортировать: " + e.getMessage()));
            }
        }).start();
    }

    private void handleCleanArchive() {
        TextInputDialog pwdDialog = new TextInputDialog();
        pwdDialog.setTitle("Подтверждение");
        pwdDialog.setHeaderText("Введите пароль для очистки архива");
        pwdDialog.setContentText("Пароль:");

        Optional<String> pwdResult = pwdDialog.showAndWait();
        if (pwdResult.isEmpty()) return;
        if (!"ktgs228".equals(pwdResult.get().trim())) {
            new Alert(Alert.AlertType.ERROR, "Неверный пароль. Доступ запрещён.")
                    .showAndWait();
            return;
        }
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Очистить архив");
        alert.setHeaderText("Удалить записи за период (от сегодня назад):");

        ButtonType dayBtn    = new ButtonType("1 день");
        ButtonType weekBtn   = new ButtonType("1 неделя");
        ButtonType monthBtn  = new ButtonType("1 месяц");
        ButtonType yearBtn   = new ButtonType("1 год");
        ButtonType allBtn    = new ButtonType("Всё");
        ButtonType cancelBtn = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(dayBtn, weekBtn, monthBtn, yearBtn, allBtn, cancelBtn);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancelBtn) return;

        LocalDate to   = LocalDate.now();
        LocalDate from;

        if      (result.get() == dayBtn)   from = LocalDate.now();
        else if (result.get() == weekBtn)  from = LocalDate.now().minusWeeks(1);
        else if (result.get() == monthBtn) from = LocalDate.now().minusMonths(1);
        else if (result.get() == yearBtn)  from = LocalDate.now().minusYears(1);
        else                               from = LocalDate.of(2000, 1, 1); // всё

        final LocalDate finalFrom = from;
        final LocalDate finalTo   = to;

        new Thread(() -> {
            try {
                ApiService.getInstance().deleteIssuancesBetween(finalFrom, finalTo);
                Platform.runLater(() -> {
                    showAlert("Готово", "Архив очищен за период: " + finalFrom + " — " + finalTo);
                    loadArchive();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка", e.getMessage()));
            }
        }).start();
    }

    // ===== НАВИГАЦИЯ =====

    @FXML
    private void handleRowClick() {
        Item selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/item_detail.fxml"));
            Stage stage = new Stage();
            stage.setTitle(selected.getName());
            stage.setScene(new Scene(loader.load(), 500, 500));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(MainApp.getPrimaryStage());
            ((ItemDetailController) loader.getController()).setItem(selected);
            stage.showAndWait();
            loadItems();
        } catch (Exception e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/item_form.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Добавить вещь");
            stage.setScene(new Scene(loader.load(), 470, 660));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(MainApp.getPrimaryStage());
            stage.showAndWait();
            loadItems();
        } catch (Exception e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        ApiService.getInstance().setToken(null);
        try { MainApp.showLoginScreen(); }
        catch (Exception e) { statusLabel.setText("Ошибка выхода"); }
    }

    // ===== УТИЛИТЫ =====

    private boolean eq(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private <T extends Comparable<T>> Comparator<T> nullsLastComparable(TableColumn<Item, T> col) {
        return (a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return col.getSortType() == TableColumn.SortType.ASCENDING ? 1 : -1;
            if (b == null) return col.getSortType() == TableColumn.SortType.ASCENDING ? -1 : 1;
            return a.compareTo(b);
        };
    }

    private Comparator<String> nullsLastString(TableColumn<Item, String> col) {
        return (a, b) -> {
            boolean aEmpty = a == null || a.isEmpty();
            boolean bEmpty = b == null || b.isEmpty();
            if (aEmpty && bEmpty) return 0;
            if (aEmpty) return col.getSortType() == TableColumn.SortType.ASCENDING ? 1 : -1;
            if (bEmpty) return col.getSortType() == TableColumn.SortType.ASCENDING ? -1 : 1;
            return a.compareToIgnoreCase(b);
        };
    }
    private Comparator<LocalDate> nullsLastDate(TableColumn<Item, LocalDate> col) {
        return (a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return col.getSortType() == TableColumn.SortType.ASCENDING ? 1 : -1;
            if (b == null) return col.getSortType() == TableColumn.SortType.ASCENDING ? -1 : 1;
            return a.compareTo(b);
        };
    }
    private void handleDeleteIssuance(Issuance issuance) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удаление");
        confirm.setHeaderText("Удалить запись?");
        confirm.setContentText("Выдача: " + issuance.getItemName()
                + "\nКому: " + issuance.getFullName()
                + "\nДата: " + issuance.getIssuedAt());
        ButtonType yes = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
        ButtonType no  = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);

        confirm.showAndWait().ifPresent(r -> {
            if (r != yes) return;
            new Thread(() -> {
                try {
                    ApiService.getInstance().deleteIssuanceById(issuance.getId());
                    Platform.runLater(() -> {
                        allIssuances = allIssuances.stream()
                                .filter(i -> !i.getId().equals(issuance.getId()))
                                .collect(Collectors.toList());
                        applyArchiveSearch();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Ошибка", e.getMessage()));
                }
            }).start();
        });
    }
    @FXML
    private void handleShowBackups() {
        List<Map<String, Object>> backups;
        try {
            backups = ApiService.getInstance().getBackups();
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось загрузить корзину: " + e.getMessage());
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Корзина — резервные копии");
        dialog.setHeaderText("Хранятся 24 часа после удаления");

        VBox content = new VBox(8);
        content.setStyle("-fx-padding: 12;");

        if (backups.isEmpty()) {
            content.getChildren().add(new Label("Резервных копий нет"));
        } else {
            for (Map<String, Object> b : backups) {
                long id = ((Number) b.get("id")).longValue();
                String desc = (String) b.get("description");
                String deletedAt = ((String) b.get("deletedAt"))
                        .substring(0, 16).replace("T", " ");

                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 6; -fx-border-color: #ddd; -fx-border-radius: 4;");

                VBox info = new VBox(2);
                info.setPrefWidth(320);
                Label descLabel = new Label(desc);
                Label dateLabel = new Label("Удалено: " + deletedAt);
                dateLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
                info.getChildren().addAll(descLabel, dateLabel);

                Button restoreBtn = new Button("Восстановить");
                restoreBtn.setOnAction(e -> {
                    try {
                        ApiService.getInstance().restoreBackup(id);
                        content.getChildren().remove(row);
                        loadItems();
                        loadArchive();
                    } catch (Exception ex) {
                        showAlert("Ошибка", "Ошибка восстановления: " + ex.getMessage());
                    }
                });

                Button deleteBtn = new Button("Удалить навсегда");
                deleteBtn.setStyle("-fx-text-fill: #c0392b;");
                deleteBtn.setOnAction(e -> {
                    try {
                        ApiService.getInstance().deleteBackup(id);
                        content.getChildren().remove(row);
                    } catch (Exception ex) {
                        showAlert("Ошибка", ex.getMessage());
                    }
                });

                row.getChildren().addAll(info, restoreBtn, deleteBtn);
                content.getChildren().add(row);
            }
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(420);
        scroll.setPrefWidth(600);

        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}