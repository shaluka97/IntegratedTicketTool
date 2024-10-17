import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class JiraTicketCreator {

    public static void main(String[] args) {
        try {
            // Dynamically fetch ticket details
            String projectKey = ConfigManager.getProperty("project.key");
            String issueType = ConfigManager.getProperty("issuetype");  // Could be passed dynamically
            String priority = ConfigManager.getProperty("priority");
            String summary = ConfigManager.getProperty("summary");
            String description = TemplateManager.getTemplate(issueType);
            String reporterUsername = ConfigManager.getProperty("jira.username"); // Assuming the reporter is the user creating the ticket

            // Fetch the accountId of the reporter
            String reporterAccountId = getAccountId(reporterUsername);

            // Create a JiraTicket object with the necessary details
            JiraTicket ticket = new JiraTicket(projectKey, issueType, summary, description, priority);

            // Add custom fields if needed (optional)
            String labels = ConfigManager.getProperty("custom.labels");
            if (labels != null && !labels.isEmpty()) {
                ticket.setCustomField("labels", List.of(labels.split(",")));
            }
            //ticket.setCustomField("components", List.of(Map.of("name", "Backend")));
            ticket.setCustomField("assignee", Map.of("id", reporterAccountId));
            // Create the ticket in Jira
            String response = createJiraTicket(ticket.toJsonMap());
            String ticketId = extractTicketId(response);
            System.out.println("Ticket created successfully: \u001B[31m" + ticketId + "\u001B[0m");
        } catch (Exception e) {
            System.err.println("Error creating the Jira ticket: " + e.getMessage());
        }
    }

    private static String createJiraTicket(Map<String, Object> issueDetails) throws Exception {
        // Prepare the request body as JSON
        String jsonBody = new com.google.gson.Gson().toJson(Map.of("fields", issueDetails));

        // Load the authentication details from the config
        String jiraUrl = ConfigManager.getProperty("jira.url");
        String username = ConfigManager.getProperty("jira.username");
        String apiToken = ConfigManager.getProperty("jira.apiToken");

        // Create a basic authentication header
        String authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + apiToken).getBytes());

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

    private static String getAccountId(String username) throws Exception {
        // Load the authentication details from the config
        String jiraUrl = ConfigManager.getProperty("jira.url");
        String apiToken = ConfigManager.getProperty("jira.apiToken");

        // Create a basic authentication header
        String authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + apiToken).getBytes());

        // Build the HTTP request to fetch user accountId
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jiraUrl + "/rest/api/3/user/search?query=" + username))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        // Send the HTTP request
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Check the response status
        if (response.statusCode() == 200) {
            // Parse the response to get the accountId
            com.google.gson.JsonArray users = new com.google.gson.JsonParser().parse(response.body()).getAsJsonArray();
            if (users.size() > 0) {
                return users.get(0).getAsJsonObject().get("accountId").getAsString();
            } else {
                throw new Exception("No user found with username: " + username);
            }
        } else {
            throw new Exception("Failed to fetch user accountId. Response code: " + response.statusCode() + ", Body: " + response.body());
        }
    }
    private static String extractTicketId(String responseBody) {
        com.google.gson.JsonObject jsonObject = new com.google.gson.JsonParser().parse(responseBody).getAsJsonObject();
        return jsonObject.get("key").getAsString();
    }
}