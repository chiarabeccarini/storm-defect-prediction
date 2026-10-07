package it.uniroma2.isw2.storm.release;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ReleaseCsvWriter {

    static final String HEADER = "Index,Version ID,Version Name,Date";

    private ReleaseCsvWriter() {
    }

    public static void write(List<Release> releases, Path csvFile) throws IOException {
        if (releases.isEmpty()) {
            throw new IllegalArgumentException("Nessuna release da scrivere in " + csvFile);
        }
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (int position = 0; position < releases.size(); position++) {
            lines.add(toCsvLine(position + 1, releases.get(position)));
        }
        Files.createDirectories(csvFile.toAbsolutePath().getParent());
        Files.writeString(csvFile, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    // I campi non vengono quotati: si rifiutano i nomi che renderebbero ambiguo il CSV.
    private static String toCsvLine(int index, Release release) {
        if (release.name().contains(",") || release.name().contains("\"")) {
            throw new IllegalArgumentException("Nome di versione non scrivibile senza quotatura: " + release.name());
        }
        return String.join(",", String.valueOf(index), release.jiraId(), release.name(),
                release.releaseDate().toString());
    }
}
