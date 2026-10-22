package it.uniroma2.isw2.storm.release;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

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
        List<IncludedRelease> includedReleases = readMainline(csvFile).stream()
                .filter(MainlineRelease::included)
                .map(MainlineRelease::toIncludedRelease)
                .toList();
        if (includedReleases.isEmpty()) {
            throw new IllegalArgumentException("Nessuna release inclusa in " + csvFile);
        }
        return includedReleases;
    }

    public static List<MainlineRelease> readMainline(Path csvFile) throws IOException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(SelectedReleaseCsvWriter.HEADER)) {
            throw new IllegalArgumentException("Intestazione inattesa in " + csvFile);
        }
        List<MainlineRelease> releases = lines.stream().skip(1).map(line -> toMainlineRelease(line, csvFile)).toList();
        if (releases.isEmpty()) {
            throw new IllegalArgumentException("Nessuna release in " + csvFile);
        }
        return releases;
    }

    private static MainlineRelease toMainlineRelease(String line, Path csvFile) {
        String[] fields = line.split(",", -1);
        if (fields.length != COLUMNS) {
            throw new IllegalArgumentException("Riga non valida in " + csvFile + ": " + line);
        }
        try {
            return new MainlineRelease(Integer.parseInt(fields[RELEASE_ID]), fields[VERSION_NAME],
                    LocalDate.parse(fields[DATE]), fields[COMMIT], fields[INCLUDED].equals("yes"));
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new IllegalArgumentException("Release ID o data non validi in " + csvFile + ": " + line, e);
        }
    }
}
