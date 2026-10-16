package it.uniroma2.isw2.storm.smells;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ClassSmellsCsvWriter {

    private static final String HEADER = "Release ID,Class,NSmells";

    private ClassSmellsCsvWriter() {
    }

    public static void write(List<ClassSmells> rows, Path csvFile) throws IOException {
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Nessun conteggio di smell da scrivere in " + csvFile);
        }
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        rows.forEach(row -> lines.add(toCsvLine(row)));
        Files.createDirectories(csvFile.toAbsolutePath().getParent());
        Files.writeString(csvFile, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    private static String toCsvLine(ClassSmells row) {
        if (row.classPath().contains(",")) {
            throw new IllegalArgumentException("Percorso non scrivibile senza quotatura: " + row.classPath());
        }
        return row.releaseId() + "," + row.classPath() + "," + row.smells();
    }
}
