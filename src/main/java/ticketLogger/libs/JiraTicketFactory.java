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
            String fieldType = getFieldType(entry.getKey());
            if (fieldType != null) {
                switch (fieldType) {
                    case "array":
                        String[] arrayValues = entry.getValue().split(",");
                        ticket.setCustomField(entry.getKey(), Arrays.asList(arrayValues));
                        break;
                    case "user":
                        if (entry.getKey().equals("assignee")) {
                            String reporterUsername = ConfigManager.getProperty("jira.username");
                            String reporterAccountId = getAccountId(reporterUsername);
                            ticket.setCustomField(entry.getKey(), Map.of("id", reporterAccountId));
                        }
                        break;
                    default:
                        ticket.setCustomField(entry.getKey(), entry.getValue());
                        break;
                }
            } else {
                ticket.setCustomField(entry.getKey(), entry.getValue());
            }
        }

        return ticket;
    }

    private static String getFieldType(String fieldName) throws Exception {
        // Load the authentication details from the config
        String jiraUrl = ConfigManager.getProperty("jira.url");
        String apiToken = ConfigManager.getProperty("jira.apiToken");
        String personalAccessToken = ConfigManager.getProperty("jira.personalAccessToken");
        String authHeader;

        // Create a basic authentication header
        if (!apiToken.isEmpty() & personalAccessToken.isEmpty()) {
            authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString((ConfigManager.getProperty("jira.username") + ":" + apiToken).getBytes());
        } else {
            authHeader = "Bearer " + personalAccessToken;
        }

        // Build the HTTP request to fetch field metadata
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jiraUrl + "/rest/api/2/field"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        // Send the HTTP request
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Check the response status
        if (response.statusCode() == 200) {
            // Parse the response to get the field type
            com.google.gson.JsonArray fields = new com.google.gson.JsonParser().parse(response.body()).getAsJsonArray();
            for (com.google.gson.JsonElement field : fields) {
                com.google.gson.JsonObject fieldObject = field.getAsJsonObject();
                if (fieldObject.get("name").getAsString().equals(fieldName)) {
                    return fieldObject.get("schema").getAsJsonObject().get("type").getAsString();
                }
            }
        } else {
            throw new Exception("Failed to fetch field metadata. Response code: " + response.statusCode() + ", Body: " + response.body());
        }

        return null;
    }

    private static String getAccountId(String username) throws Exception {
        // Load the authentication details from the config
        String jiraUrl = ConfigManager.getProperty("jira.url");
        String apiToken = ConfigManager.getProperty("jira.apiToken");
        String personalAccessToken = ConfigManager.getProperty("jira.personalAccessToken");
        String authHeader;

        // Create a basic authentication header
        if (!apiToken.isEmpty() & personalAccessToken.isEmpty()) {
            authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + apiToken).getBytes());
        } else {
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