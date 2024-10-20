package ticketLogger.libs;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Map;

public class JiraTicketFactory {

    public static JiraTicket createTicket(String taskType) throws Exception {
        String projectKey = ConfigManager.getProperty("project.key");
        String priority = ConfigManager.getProperty("priority");
        String summary = ConfigManager.getProperty("summary");
        String description = TemplateManager.getTemplate(taskType);

        String summaryTag = ConfigManager.getProperty(taskType + ".summarytag");
        if (summaryTag != null) {
            summary = summaryTag + " " + summary;
        }

        JiraTicket ticket = new JiraTicket(projectKey, taskType, summary, description, priority);

        // Add custom fields based on task type
        Map<String, String> customFields = ConfigManager.getPropertiesByPrefix("custom." + taskType + ".");
        for (Map.Entry<String, String> entry : customFields.entrySet()) {
            if (entry.getKey().equals("labels")) {
                // Convert labels to an array of strings
                String[] labelsArray = entry.getValue().split(",");
                ticket.setCustomField(entry.getKey(), Arrays.asList(labelsArray));
            } else {
                ticket.setCustomField(entry.getKey(), entry.getValue());
            }
        }

        String reporterUsername = ConfigManager.getProperty("jira.username"); // Assuming the reporter is the user creating the ticket
        // Fetch the accountId of the reporter
        String reporterAccountId = getAccountId(reporterUsername);
        //ticket.setCustomField("components", List.of(Map.of("name", "Backend")));
        ticket.setCustomField("assignee", Map.of("id", reporterAccountId));

        return ticket;
    }

    private static String getAccountId(String username) throws Exception {
        // Load the authentication details from the config
        String jiraUrl = ConfigManager.getProperty("jira.url");
        String apiToken = ConfigManager.getProperty("jira.apiToken");
        String personalAccessToken = ConfigManager.getProperty("jira.personalAccessToken");
        String authHeader;

        // Create a basic authentication header
        if (apiToken != null) {
            authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + apiToken).getBytes());
        }else{
            authHeader = "Bearer " + personalAccessToken;
        }
        // Build the HTTP request to fetch user accountId
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jiraUrl + "/rest/api/2/user/search?query=" + username))
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
            if (!users.isEmpty()) {
                return users.get(0).getAsJsonObject().get("accountId").getAsString();
            } else {
                throw new Exception("No user found with username: " + username);
            }
        } else {
            throw new Exception("Failed to fetch user accountId. Response code: " + response.statusCode() + ", Body: " + response.body());
        }
    }
}