package it.uniroma2.isw2.storm.jira;

import it.uniroma2.isw2.storm.ticket.Ticket;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public final class TicketExtractor {

    public static final List<String> FIELDS = List.of("key", "created", "resolutiondate", "versions");

    private static final DateTimeFormatter JIRA_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private TicketExtractor() {
    }

    public static List<Ticket> tickets(List<JSONObject> issues) {
        return issues.stream()
                .map(TicketExtractor::toTicket)
                .sorted(Comparator.comparingInt(TicketExtractor::keyNumber))
                .toList();
    }

    private static Ticket toTicket(JSONObject issue) {
        String key = issue.getString("key");
        JSONObject fields = issue.getJSONObject("fields");
        return new Ticket(key, timestamp(fields, "created", key), timestamp(fields, "resolutiondate", key),
                affectedVersions(fields.optJSONArray("versions")));
    }

    private static OffsetDateTime timestamp(JSONObject fields, String field, String key) {
        if (fields.isNull(field)) {
            throw new IllegalArgumentException("Campo " + field + " mancante nel ticket " + key);
        }
        try {
            return OffsetDateTime.parse(fields.getString(field), JIRA_TIMESTAMP);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data non valida nel campo " + field + " del ticket " + key, e);
        }
    }

    private static List<String> affectedVersions(JSONArray versions) {
        if (versions == null) {
            return List.of();
        }
        return IntStream.range(0, versions.length())
                .mapToObj(position -> versions.getJSONObject(position).getString("name"))
                .toList();
    }

    private static int keyNumber(Ticket ticket) {
        return Integer.parseInt(ticket.key().substring(ticket.key().lastIndexOf('-') + 1));
    }
}
