package ticketLogger.libs;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class JiraTicketCreator {

    public static String createJiraTicket(Map<String, Object> issueDetails) throws Exception {
        // Prepare the request body as JSON
        String jsonBody = new com.google.gson.Gson().toJson(Map.of("fields", issueDetails));

        // Load the authentication details from the config
        String jiraUrl = ConfigManager.getProperty("jira.url");
        String username = ConfigManager.getProperty("jira.username");
        String apiToken = ConfigManager.getProperty("jira.apiToken");
        String personalAccessToken = ConfigManager.getProperty("jira.personalAccessToken");
        String authHeader;

        // Create a basic authentication header
        if (apiToken != null) {
            authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + apiToken).getBytes());
        }else {
            authHeader = "Bearer" + personalAccessToken;
        }
        // Build the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jiraUrl + "/rest/api/2/issue"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // Send the HTTP request
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Check the response status
        if (response.statusCode() == 201) {
            return response.body();  // Ticket created successfully
        } else {
            throw new Exception("Failed to create ticket. Response code: " + response.statusCode() + ", Body: " + response.body());
        }
    }

    public static String extractTicketId(String responseBody) {
        com.google.gson.JsonObject jsonObject = new com.google.gson.JsonParser().parse(responseBody).getAsJsonObject();
        return jsonObject.get("key").getAsString();
    }
}