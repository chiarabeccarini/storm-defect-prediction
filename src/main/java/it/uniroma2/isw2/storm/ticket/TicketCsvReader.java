package it.uniroma2.isw2.storm.ticket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class TicketCsvReader {

    private TicketCsvReader() {
    }

    public static Set<String> readKeys(Path csvFile) throws IOException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(TicketCsvWriter.HEADER)) {
            throw new IllegalArgumentException("Intestazione inattesa in " + csvFile);
        }
        Set<String> keys = lines.stream().skip(1)
                .map(line -> line.split(",", -1)[0])
                .collect(Collectors.toUnmodifiableSet());
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("Nessun ticket in " + csvFile);
        }
        return keys;
    }
}
