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
        // Send the HTTP request
        HttpResponse<String> response = JiraHttpClient.sendPostRequest("/rest/api/2/issue", jsonBody);
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