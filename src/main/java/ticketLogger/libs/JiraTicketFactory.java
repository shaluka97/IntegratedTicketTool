package ticketLogger.libs;

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
        HttpResponse<String> response = JiraHttpClient.sendGetRequest("/rest/api/2/field");

        // Check the response status
        if (response.statusCode() == 200) {
            // Parse the response to get the field type
            com.google.gson.JsonArray fields = new com.google.gson.JsonParser().parse(response.body()).getAsJsonArray();
            for (com.google.gson.JsonElement field : fields) {
                com.google.gson.JsonObject fieldObject = field.getAsJsonObject();
                if (fieldObject.get("key").getAsString().equals(fieldName)) {
                    return fieldObject.get("schema").getAsJsonObject().get("type").getAsString();
                }
            }
        } else {
            throw new Exception("Failed to fetch field metadata. Response code: " + response.statusCode() + ", Body: " + response.body());
        }

        return null;
    }

    private static String getAccountId(String username) throws Exception {
        HttpResponse<String> response = JiraHttpClient.sendGetRequest("/rest/api/2/user/search?query=" + username);

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