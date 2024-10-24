package ticketLogger;

import ticketLogger.libs.ConfigManager;
import ticketLogger.libs.JiraTicket;
import ticketLogger.libs.JiraTicketCreator;
import ticketLogger.libs.JiraTicketFactory;

import java.util.Map;
import java.util.Optional;

public class TicketLoggerMain {

    public static void main(String[] args) {
        try {
            String taskType = ConfigManager.getProperty("issuetype");
            JiraTicket ticket = JiraTicketFactory.createTicket(taskType);

            // Create the main ticket in Jira
            String response = JiraTicketCreator.createJiraTicket(ticket.toJsonMap());
            String ticketId = JiraTicketCreator.extractTicketId(response);
            System.out.println("Ticket created successfully: \u001B[31m" + ticketId + "\u001B[0m");

            // Extract the assignee from the main ticket
            Optional<Map<String, String>> assignee = Optional.ofNullable((Map<String, String>) ticket.toJsonMap().get("assignee"));

            // Create sub-tasks if the main task is of type "Task"
            if ("Task".equalsIgnoreCase(taskType)) {
                String[] subTaskNames = ConfigManager.getProperty("subtasks.Task").split(",");
                for (String subTaskName : subTaskNames) {
                    JiraTicket subTask = ticket.createSubTask(subTaskName.trim());
                    if (assignee.isPresent()) {
                        subTask.setCustomField("assignee", assignee);
                    }
                    JiraTicketCreator.createJiraSubTask(subTask.toJsonMap(), ticketId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating the Jira ticket: " + e.getMessage());
        }
    }
}