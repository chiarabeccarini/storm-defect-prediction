package it.uniroma2.isw2.storm.ticket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class TicketCsvReader {

    private static final int COLUMNS = 4;

    private TicketCsvReader() {
    }

    public static Set<String> readKeys(Path csvFile) throws IOException {
        return read(csvFile).stream().map(Ticket::key).collect(Collectors.toUnmodifiableSet());
    }

    public static List<Ticket> read(Path csvFile) throws IOException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(TicketCsvWriter.HEADER)) {
            throw new IllegalArgumentException("Intestazione inattesa in " + csvFile);
        }
        List<Ticket> tickets = lines.stream().skip(1).map(line -> toTicket(line, csvFile)).toList();
        if (tickets.isEmpty()) {
            throw new IllegalArgumentException("Nessun ticket in " + csvFile);
        }
        return tickets;
    }

    private static Ticket toTicket(String line, Path csvFile) {
        String[] fields = line.split(",", -1);
        if (fields.length != COLUMNS) {
            throw new IllegalArgumentException("Riga non valida in " + csvFile + ": " + line);
        }
        List<String> affectedVersions = Arrays.stream(fields[3].split(TicketCsvWriter.VERSION_SEPARATOR))
                .filter(version -> !version.isBlank())
                .toList();
        try {
            return new Ticket(fields[0], OffsetDateTime.parse(fields[1]), OffsetDateTime.parse(fields[2]), affectedVersions);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data non valida in " + csvFile + ": " + line, e);
        }
    }
}
