package it.uniroma2.isw2.storm.ticket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TicketCsvWriter {

    static final String HEADER = "Key,Created,Resolved,Affected Versions";
    private static final String VERSION_SEPARATOR = ";";

    private TicketCsvWriter() {
    }

    public static void write(List<Ticket> tickets, Path csvFile) throws IOException {
        if (tickets.isEmpty()) {
            throw new IllegalArgumentException("Nessun ticket da scrivere in " + csvFile);
        }
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        tickets.forEach(ticket -> lines.add(toCsvLine(ticket)));
        Files.createDirectories(csvFile.toAbsolutePath().getParent());
        Files.writeString(csvFile, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    // Più Affected Version stanno nello stesso campo separate da ';': si rifiutano i nomi che lo renderebbero ambiguo.
    private static String toCsvLine(Ticket ticket) {
        for (String version : ticket.affectedVersions()) {
            if (version.contains(",") || version.contains(VERSION_SEPARATOR) || version.contains("\"")) {
                throw new IllegalArgumentException("Versione non scrivibile senza quotatura nel ticket " + ticket.key());
            }
        }
        return String.join(",", ticket.key(), ticket.created().toString(), ticket.resolved().toString(),
                String.join(VERSION_SEPARATOR, ticket.affectedVersions()));
    }
}
