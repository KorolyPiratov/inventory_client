package inventory_client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResponse {
    private String token;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}