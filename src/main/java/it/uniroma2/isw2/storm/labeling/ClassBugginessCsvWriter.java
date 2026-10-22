package it.uniroma2.isw2.storm.labeling;

import it.uniroma2.isw2.storm.classes.ReleaseClass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ClassBugginessCsvWriter {

    private static final String HEADER = "Release ID,Class,Bugginess";

    private ClassBugginessCsvWriter() {
    }

    public static void write(List<ReleaseClass> releaseClasses, Set<ReleaseClass> buggyClasses, Path csvFile) throws IOException {
        if (releaseClasses.isEmpty()) {
            throw new IllegalArgumentException("Nessuna classe da etichettare in " + csvFile);
        }
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        releaseClasses.forEach(releaseClass -> lines.add(toCsvLine(releaseClass, buggyClasses.contains(releaseClass))));
        Files.createDirectories(csvFile.toAbsolutePath().getParent());
        Files.writeString(csvFile, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    // Valori Yes/No come nel dataset di esempio fornito a lezione.
    private static String toCsvLine(ReleaseClass releaseClass, boolean buggy) {
        if (releaseClass.path().contains(",")) {
            throw new IllegalArgumentException("Percorso non scrivibile senza quotatura: " + releaseClass.path());
        }
        return releaseClass.releaseId() + "," + releaseClass.path() + "," + (buggy ? "Yes" : "No");
    }
}
