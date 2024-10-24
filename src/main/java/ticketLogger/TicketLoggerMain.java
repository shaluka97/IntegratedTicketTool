package ticketLogger;

import ticketLogger.libs.ConfigManager;
import ticketLogger.libs.JiraTicket;
import ticketLogger.libs.JiraTicketCreator;
import ticketLogger.libs.JiraTicketFactory;

public class TicketLoggerMain {

    public static void main(String[] args) {
        try {
            String taskType = ConfigManager.getProperty("issuetype");
            JiraTicket ticket = JiraTicketFactory.createTicket(taskType);

            // Create the main ticket in Jira
            String response = JiraTicketCreator.createJiraTicket(ticket.toJsonMap());
            String ticketId = JiraTicketCreator.extractTicketId(response);
            System.out.println("Ticket created successfully: \u001B[31m" + ticketId + "\u001B[0m");

            // Create sub-tasks if the main task is of type "Task"
            if ("Task".equalsIgnoreCase(taskType)) {
                String[] subTaskNames = ConfigManager.getProperty("subtasks.Task").split(",");
                for (String subTaskName : subTaskNames) {
                    JiraTicket subTask = ticket.createSubTask(subTaskName.trim());
                    JiraTicketCreator.createJiraSubTask(subTask.toJsonMap(), ticketId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating the Jira ticket: " + e.getMessage());
        }
    }
}