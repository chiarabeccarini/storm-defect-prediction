package it.uniroma2.isw2.storm.classes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ReleaseClassCsvReader {

    private ReleaseClassCsvReader() {
    }

    public static List<ReleaseClass> read(Path csvFile) throws IOException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(ReleaseClassCsvWriter.HEADER)) {
            throw new IllegalArgumentException("Intestazione inattesa in " + csvFile);
        }
        return lines.stream().skip(1).map(line -> toReleaseClass(line, csvFile)).toList();
    }

    private static ReleaseClass toReleaseClass(String line, Path csvFile) {
        String[] fields = line.split(",", -1);
        if (fields.length != 2) {
            throw new IllegalArgumentException("Riga non valida in " + csvFile + ": " + line);
        }
        try {
            return new ReleaseClass(Integer.parseInt(fields[0]), fields[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Release ID non numerico in " + csvFile + ": " + line, e);
        }
    }
}
