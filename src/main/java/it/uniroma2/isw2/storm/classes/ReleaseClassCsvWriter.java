package it.uniroma2.isw2.storm.classes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ReleaseClassCsvWriter {

    private static final String HEADER = "Release ID,Class";

    private ReleaseClassCsvWriter() {
    }

    public static void write(List<ReleaseClass> releaseClasses, Path csvFile) throws IOException {
        if (releaseClasses.isEmpty()) {
            throw new IllegalArgumentException("Nessuna classe da scrivere in " + csvFile);
        }
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        releaseClasses.forEach(releaseClass -> lines.add(toCsvLine(releaseClass)));
        Files.createDirectories(csvFile.toAbsolutePath().getParent());
        Files.writeString(csvFile, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    // I campi non vengono quotati: si rifiutano i percorsi che renderebbero ambiguo il CSV.
    private static String toCsvLine(ReleaseClass releaseClass) {
        if (releaseClass.path().contains(",") || releaseClass.path().contains("\"")) {
            throw new IllegalArgumentException("Percorso non scrivibile senza quotatura: " + releaseClass.path());
        }
        return releaseClass.releaseId() + "," + releaseClass.path();
    }
}
