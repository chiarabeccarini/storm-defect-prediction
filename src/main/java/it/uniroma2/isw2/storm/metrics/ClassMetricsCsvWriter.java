package it.uniroma2.isw2.storm.metrics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ClassMetricsCsvWriter {

    private static final String RELEASE_SUFFIX = "_Release";
    private static final String TOTAL_SUFFIX = "_Total";

    // Metriche con asterisco: ognuna produce la colonna della release e quella dell'intera storia.
    private static final List<Map.Entry<String, Function<WindowMetrics, String>>> WINDOWED_METRICS = List.of(
            Map.entry("LOC_Touched", window -> String.valueOf(window.churn().sum())),
            Map.entry("NR", window -> String.valueOf(window.revisions())),
            Map.entry("Nfix", window -> String.valueOf(window.fixes())),
            Map.entry("Nauth", window -> String.valueOf(window.authors())),
            Map.entry("LOC_Added", window -> String.valueOf(window.addedLines().sum())),
            Map.entry("Max_LOC_Added", window -> String.valueOf(window.addedLines().max())),
            Map.entry("Avg_LOC_Added", window -> twoDecimals(window.addedLines().average())),
            Map.entry("Churn", window -> String.valueOf(window.churn().sum())),
            Map.entry("Max_Churn", window -> String.valueOf(window.churn().max())),
            Map.entry("Avg_Churn", window -> twoDecimals(window.churn().average())),
            Map.entry("ChgSetSize", window -> String.valueOf(window.changeSet().sum())),
            Map.entry("Max_ChgSet", window -> String.valueOf(window.changeSet().max())),
            Map.entry("Avg_ChgSet", window -> twoDecimals(window.changeSet().average())));

    private ClassMetricsCsvWriter() {
    }

    public static void write(List<ClassMetrics> classMetrics, Path csvFile) throws IOException {
        if (classMetrics.isEmpty()) {
            throw new IllegalArgumentException("Nessuna metrica da scrivere in " + csvFile);
        }
        List<String> lines = new ArrayList<>();
        lines.add(header());
        classMetrics.forEach(metrics -> lines.add(toCsvLine(metrics)));
        Files.createDirectories(csvFile.toAbsolutePath().getParent());
        Files.writeString(csvFile, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    private static String header() {
        String windowed = WINDOWED_METRICS.stream()
                .map(metric -> metric.getKey() + RELEASE_SUFFIX + "," + metric.getKey() + TOTAL_SUFFIX)
                .collect(Collectors.joining(","));
        return "Release ID,Class,LOC" + RELEASE_SUFFIX + ",LOC" + TOTAL_SUFFIX + "," + windowed + ",Age,WeightedAge";
    }

    // La dimensione è una proprietà dello snapshot: le due colonne LOC hanno lo stesso valore.
    private static String toCsvLine(ClassMetrics metrics) {
        if (metrics.classPath().contains(",")) {
            throw new IllegalArgumentException("Percorso non scrivibile senza quotatura: " + metrics.classPath());
        }
        String windowed = WINDOWED_METRICS.stream()
                .map(metric -> metric.getValue().apply(metrics.release()) + "," + metric.getValue().apply(metrics.total()))
                .collect(Collectors.joining(","));
        return String.join(",", String.valueOf(metrics.releaseId()), metrics.classPath(),
                String.valueOf(metrics.loc()), String.valueOf(metrics.loc()), windowed,
                String.valueOf(metrics.ageWeeks()), String.valueOf(metrics.weightedAge()));
    }

    private static String twoDecimals(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
