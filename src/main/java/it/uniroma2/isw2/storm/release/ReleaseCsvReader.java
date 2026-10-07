package it.uniroma2.isw2.storm.release;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class ReleaseCsvReader {

    private ReleaseCsvReader() {
    }

    public static List<Release> read(Path csvFile) throws IOException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(ReleaseCsvWriter.HEADER)) {
            throw new IllegalArgumentException("Intestazione inattesa in " + csvFile);
        }
        return lines.stream().skip(1).map(line -> toRelease(line, csvFile)).toList();
    }

    private static Release toRelease(String line, Path csvFile) {
        String[] fields = line.split(",", -1);
        if (fields.length != 4) {
            throw new IllegalArgumentException("Riga non valida in " + csvFile + ": " + line);
        }
        try {
            return new Release(fields[1], fields[2], LocalDate.parse(fields[3]));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data non valida in " + csvFile + ": " + line, e);
        }
    }
}
