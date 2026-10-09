package it.uniroma2.isw2.storm.release;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public final class IncludedReleaseCsvReader {

    private static final int RELEASE_ID = 0;
    private static final int VERSION_NAME = 1;
    private static final int DATE = 2;
    private static final int COMMIT = 4;
    private static final int INCLUDED = 5;
    private static final int COLUMNS = 6;

    private IncludedReleaseCsvReader() {
    }

    public static List<IncludedRelease> read(Path csvFile) throws IOException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(SelectedReleaseCsvWriter.HEADER)) {
            throw new IllegalArgumentException("Intestazione inattesa in " + csvFile);
        }
        List<IncludedRelease> includedReleases = lines.stream().skip(1)
                .map(line -> toIncludedRelease(line, csvFile))
                .flatMap(Optional::stream)
                .toList();
        if (includedReleases.isEmpty()) {
            throw new IllegalArgumentException("Nessuna release inclusa in " + csvFile);
        }
        return includedReleases;
    }

    private static Optional<IncludedRelease> toIncludedRelease(String line, Path csvFile) {
        String[] fields = line.split(",", -1);
        if (fields.length != COLUMNS) {
            throw new IllegalArgumentException("Riga non valida in " + csvFile + ": " + line);
        }
        if (!fields[INCLUDED].equals("yes")) {
            return Optional.empty();
        }
        try {
            return Optional.of(new IncludedRelease(Integer.parseInt(fields[RELEASE_ID]), fields[VERSION_NAME],
                    LocalDate.parse(fields[DATE]), fields[COMMIT]));
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new IllegalArgumentException("Release ID o data non validi in " + csvFile + ": " + line, e);
        }
    }
}
