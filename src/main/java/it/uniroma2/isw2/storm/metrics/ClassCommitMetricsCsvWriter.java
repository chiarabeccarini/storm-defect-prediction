package it.uniroma2.isw2.storm.metrics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClassCommitMetricsCsvWriter {

    private static final String HEADER = "Release ID,Class,NS,ND,NF,Entropy,LA,LD,LT,NDEV,AGE_Commit,NUC,EXP,REXP,SEXP";

    private ClassCommitMetricsCsvWriter() {
    }

    public static void write(List<ClassCommitMetrics> rows, Path csvFile) throws IOException {
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Nessuna metrica dei commit da scrivere in " + csvFile);
        }
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        rows.forEach(row -> lines.add(toCsvLine(row)));
        Files.createDirectories(csvFile.toAbsolutePath().getParent());
        Files.writeString(csvFile, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    private static String toCsvLine(ClassCommitMetrics row) {
        if (row.classPath().contains(",")) {
            throw new IllegalArgumentException("Percorso non scrivibile senza quotatura: " + row.classPath());
        }
        CommitMetrics sums = row.commitSums();
        return String.join(",", String.valueOf(row.releaseId()), row.classPath(),
                String.valueOf(sums.subsystems()), String.valueOf(sums.directories()), String.valueOf(sums.files()),
                twoDecimals(sums.entropy()), String.valueOf(sums.addedLines()), String.valueOf(sums.deletedLines()),
                String.valueOf(row.linesBeforeChanges()), String.valueOf(sums.developers()), twoDecimals(sums.ageWeeks()),
                String.valueOf(sums.uniqueChanges()), String.valueOf(sums.experience()), twoDecimals(sums.recentExperience()),
                String.valueOf(sums.subsystemExperience()));
    }

    private static String twoDecimals(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
