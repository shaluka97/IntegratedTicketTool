package ticketLogger.libs;

import java.util.HashMap;
import java.util.Map;

public class JiraTicket {
    private final String projectKey;
    private final String issueType;
    private final String summary;
    private final String description;
    private final String priority;
    private final Map<String, Object> customFields = new HashMap<>();

    public JiraTicket(String projectKey, String issueType, String summary, String description, String priority) {
        this.projectKey = projectKey;
        this.issueType = issueType;
        this.summary = summary;
        this.description = description;
        this.priority = priority;
    }

    // Getters and setters
    public void setCustomField(String fieldName, Object value) {
        this.customFields.put(fieldName, value);
    }

    public Map<String, Object> toJsonMap() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("project", Map.of("key", projectKey));
        fields.put("issuetype", Map.of("name", issueType));
        fields.put("summary", summary);
        fields.put("priority", Map.of("name", priority));
        fields.put("description", description);
        fields.putAll(customFields);  // Add custom fields dynamically
        return fields;
    }

    public JiraTicket createSubTask(String summary) {
        return new JiraTicket(this.projectKey, "Sub-task", summary, "", this.priority);
    }

}
