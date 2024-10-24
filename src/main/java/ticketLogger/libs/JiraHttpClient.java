package ticketLogger.libs;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class JiraHttpClient {

    public static HttpResponse<String> sendGetRequest(String endpoint) throws Exception {
        String jiraUrl = ConfigManager.getProperty("jira.url");
        String apiToken = ConfigManager.getProperty("jira.apiToken");
        String personalAccessToken = ConfigManager.getProperty("jira.personalAccessToken");
        String authHeader;

        // Create a basic authentication header
        if (!apiToken.isEmpty() && personalAccessToken.isEmpty()) {
            authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString((ConfigManager.getProperty("jira.username") + ":" + apiToken).getBytes());
        } else {
            authHeader = "Bearer " + personalAccessToken;
        }

        // Build the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jiraUrl + endpoint))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        // Send the HTTP request
        HttpClient client = HttpClient.newHttpClient();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> sendPostRequest(String endpoint, String jsonBody) throws Exception {
        String jiraUrl = ConfigManager.getProperty("jira.url");
        String apiToken = ConfigManager.getProperty("jira.apiToken");
        String personalAccessToken = ConfigManager.getProperty("jira.personalAccessToken");
        String authHeader;

        // Create a basic authentication header
        if (!apiToken.isEmpty() && personalAccessToken.isEmpty()) {
            authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString((ConfigManager.getProperty("jira.username") + ":" + apiToken).getBytes());
        } else {
            authHeader = "Bearer " + personalAccessToken;
        }

        // Build the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jiraUrl + endpoint))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // Send the HTTP request
        HttpClient client = HttpClient.newHttpClient();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}