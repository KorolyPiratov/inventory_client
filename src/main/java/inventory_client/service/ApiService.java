package inventory_client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import inventory_client.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ApiService {

    private String baseUrl;
    private static ApiService instance;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String token;

    private ApiService() {
        this.httpClient = HttpClient.newHttpClient();
        this.baseUrl = "http://" + System.getProperty("SERVER_IP", "localhost") + ":8080";
        this.objectMapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void setServerIp(String ip) {
        this.baseUrl = "http://" + ip + ":8080";
        this.token = null; // сбрасываем токен — нужна повторная авторизация
    }

    public String getServerIp() {
        return baseUrl.replace("http://", "").replace(":8080", "");
    }

    // Новый метод — удалить все вещи
    public void deleteAllItems() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/items"))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new Exception("Ошибка удаления: " + response.statusCode());
        }
    }

    public static ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    // Логин
    public String login(String username, String password) throws Exception {
        AuthRequest authRequest = new AuthRequest(username, password);
        String body = objectMapper.writeValueAsString(authRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            AuthResponse authResponse = objectMapper.readValue(
                    response.body(), AuthResponse.class);
            this.token = authResponse.getToken();
            return authResponse.getToken();
        } else {
            throw new Exception("Неверный логин или пароль");
        }
    }

    // Получить все вещи
    public List<Item> getItems() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/items"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(),
                new TypeReference<List<Item>>() {});
    }

    // Поиск вещей
    public List<Item> searchItems(String name) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/items/search?name=" + name))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(),
                new TypeReference<List<Item>>() {});
    }

    // Фильтрация
    public List<Item> filterItems(String category, String colorType,
                                  String manufacturer, String boxNumber) throws Exception {
        StringBuilder url = new StringBuilder(baseUrl + "/api/items/filter?");
        if (category != null) url.append("category=").append(category).append("&");
        if (colorType != null) url.append("colorType=").append(colorType).append("&");
        if (manufacturer != null) url.append("manufacturer=").append(manufacturer).append("&");
        if (boxNumber != null) url.append("boxNumber=").append(boxNumber).append("&");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(),
                new TypeReference<List<Item>>() {});
    }

    // Создать вещь
    public Item createItem(Item item) throws Exception {
        String body = objectMapper.writeValueAsString(item);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/items"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), Item.class);
    }

    // Обновить вещь
    public Item updateItem(Long id, Item item) throws Exception {
        String body = objectMapper.writeValueAsString(item);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/items/" + id))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), Item.class);
    }

    // Получить выдачи по вещи
    public List<Issuance> getIssuances(Long itemId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/issuances/item/" + itemId))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(),
                new TypeReference<List<Issuance>>() {});
    }

    // Выдать вещь
    public Issuance issueItem(Long itemId, String fullName,
                              Boolean isIndefinite, LocalDate returnDate) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("fullName", fullName);
        body.put("isIndefinite", isIndefinite);
        if (returnDate != null) body.put("returnDate", returnDate.toString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/issuances/item/" + itemId))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), Issuance.class);
    }

    // Получить производителей
    public List<String> getManufacturers() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/items/manufacturers"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(),
                new TypeReference<List<String>>() {});
    }
    // Получить все выдачи (архив)
    public List<Issuance> getAllIssuances() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/issuances"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(),
                new TypeReference<List<Issuance>>() {});
    }

    public void deleteItem(Long id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/items/" + id))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public void deleteIssuancesByItem(Long itemId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/issuances/item/" + itemId))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public void deleteIssuancesBetween(LocalDate from, LocalDate to) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/issuances/between?from=" + from + "&to=" + to))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
    public void deleteIssuanceById(Long id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/issuances/" + id))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
    public List<Map<String, Object>> getBackups() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/backups"))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(resp.body(), new TypeReference<>() {});
    }

    public void restoreBackup(long id) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/backups/" + id + "/restore"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public void deleteBackup(long id) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/backups/" + id))
                .header("Authorization", "Bearer " + token)
                .DELETE().build();
        httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }
}