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

            // Create the ticket in Jira
            String response = JiraTicketCreator.createJiraTicket(ticket.toJsonMap());
            String ticketId = JiraTicketCreator.extractTicketId(response);
            System.out.println("Ticket created successfully: \u001B[31m" + ticketId + "\u001B[0m");
        } catch (Exception e) {
            System.err.println("Error creating the Jira ticket: " + e.getMessage());
        }
    }
}