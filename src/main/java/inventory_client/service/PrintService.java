package inventory_client.service;

import java.util.List;
import inventory_client.model.Item;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.xwpf.usermodel.*;

import java.awt.Desktop;
import java.io.*;
import java.nio.file.*;
import java.util.Map;

public class PrintService {

    // Путь к шаблону — можно менять из настроек
    private static Path customTemplatePath = null;

    public static void setCustomTemplatePath(Path path) {
        customTemplatePath = path;
    }

    public static Path getCustomTemplatePath() {
        return customTemplatePath;
    }

    public static void printIssuance(Item item, String fullName,
                                     String date, boolean isIndefinite) {
        Map<String, String> placeholders = Map.of(
                "{{name}}",         item.getName() != null ? item.getName() : "—",
                "{{category}}",     item.getCategory() != null ? item.getCategory() : "—",
                "{{supplyDate}}", item.getSupplyDate() != null ? item.getSupplyDate().toString() : "—",
                "{{boxNumber}}",    item.getBoxNumber() != null ? item.getBoxNumber() : "—",
                "{{fullName}}",     fullName != null ? fullName : "—",
                "{{date}}",         date != null ? date : "—",
                "{{indefinite}}",   isIndefinite ? "Да" : "Нет"
        );

        new Thread(() -> {
            try {
                InputStream templateStream = resolveTemplate();
                if (templateStream == null) {
                    Platform.runLater(() -> showAlert("Ошибка",
                            "Шаблон не найден. Положите template.docx рядом с приложением " +
                                    "или укажите путь в настройках."));
                    return;
                }

                // Открываем шаблон через POI
                XWPFDocument doc = new XWPFDocument(templateStream);

                // Заменяем плейсхолдеры во всех параграфах документа
                for (XWPFParagraph paragraph : doc.getParagraphs()) {
                    replacePlaceholdersInParagraph(paragraph, placeholders);
                }

                // Таблицы тоже обрабатываем
                for (XWPFTable table : doc.getTables()) {
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                                replacePlaceholdersInParagraph(paragraph, placeholders);
                            }
                        }
                    }
                }

                // Сохраняем во временный файл
                Path tempFile = Files.createTempFile("issuance_", ".docx");
                try (OutputStream out = Files.newOutputStream(tempFile)) {
                    doc.write(out);
                }
                doc.close();

                // Открываем через Word
                Platform.runLater(() -> {
                    try {
                        Desktop.getDesktop().open(tempFile.toFile());
                    } catch (IOException e) {
                        showAlert("Ошибка", "Не удалось открыть документ: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка печати",
                        "Произошла ошибка: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Заменяет плейсхолдеры в параграфе.
     * Важно: POI может разбивать текст параграфа на несколько runs,
     * поэтому собираем полный текст, заменяем, пишем в первый run.
     */
    private static void replacePlaceholdersInParagraph(XWPFParagraph paragraph,
                                                       Map<String, String> placeholders) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) return;

        // Собираем полный текст параграфа
        StringBuilder fullText = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null) fullText.append(text);
        }

        String result = fullText.toString();
        boolean changed = false;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            if (result.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
                changed = true;
            }
        }

        if (!changed) return;

        // Пишем результат в первый run, остальные очищаем
        runs.get(0).setText(result, 0);
        for (int i = 1; i < runs.size(); i++) {
            runs.get(i).setText("", 0);
        }
    }

    /**
     * Ищет шаблон: сначала кастомный путь, потом рядом с jar, потом из ресурсов.
     */
    private static InputStream resolveTemplate() throws IOException {
        // 1. Кастомный путь из настроек
        if (customTemplatePath != null && Files.exists(customTemplatePath)) {
            return Files.newInputStream(customTemplatePath);
        }

        // 2. template.docx рядом с .jar
        Path nextToJar = Path.of("template.docx");
        if (Files.exists(nextToJar)) {
            return Files.newInputStream(nextToJar);
        }

        // 3. Дефолтный из ресурсов (resources/template.docx)
        InputStream fromResources = PrintService.class
                .getResourceAsStream("/template.docx");
        return fromResources; // может быть null — обработается выше
    }

    // Открыть диалог выбора шаблона (вызывать из UI настроек)
    public static void chooseTemplate(Stage ownerStage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите шаблон (.docx)");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Word документ", "*.docx"));

        File file = chooser.showOpenDialog(ownerStage);
        if (file != null) {
            customTemplatePath = file.toPath();
            showAlert("Шаблон выбран", "Шаблон: " + file.getName());
        }
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}