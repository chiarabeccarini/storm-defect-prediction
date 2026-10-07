package it.uniroma2.isw2.storm.release;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SelectedReleaseCsvWriter {

    static final String HEADER = "Release ID,Version Name,Date,Tag,Commit,Included";

    private SelectedReleaseCsvWriter() {
    }

    public static void write(List<SelectedRelease> selectedReleases, Path csvFile) throws IOException {
        if (selectedReleases.isEmpty()) {
            throw new IllegalArgumentException("Nessuna release selezionata da scrivere in " + csvFile);
        }
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        selectedReleases.forEach(selected -> lines.add(toCsvLine(selected)));
        Files.createDirectories(csvFile.toAbsolutePath().getParent());
        Files.writeString(csvFile, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    private static String toCsvLine(SelectedRelease selected) {
        Release release = selected.release();
        return String.join(",", String.valueOf(selected.releaseId()), release.name(),
                release.releaseDate().toString(), selected.tag(), selected.commit(), selected.included() ? "yes" : "no");
    }
}
