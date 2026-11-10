package it.uniroma2.isw2.storm.prediction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Rilegge il CSV dei fold della cross-validation: per ogni schema, i valori di ogni metrica nei 100 fold. */
public final class ResultsCsvReader {

    private static final String SCHEME_COLUMN = "Scheme";

    private ResultsCsvReader() {
    }

    /** Le chiavi sono i nomi degli schemi nell'ordine del file; i valori non definiti restano NaN. */
    public static Map<String, Map<Metric, List<Double>>> readFolds(Path csvFile) throws IOException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.size() < 2) {
            throw new IllegalArgumentException("Nessun risultato in " + csvFile);
        }
        List<String> header = Arrays.asList(lines.get(0).split(",", -1));
        int schemeColumn = columnOf(header, SCHEME_COLUMN, csvFile);
        Map<Metric, Integer> metricColumns = new EnumMap<>(Metric.class);
        for (Metric metric : Metric.values()) {
            metricColumns.put(metric, columnOf(header, metric.label(), csvFile));
        }
        Map<String, Map<Metric, List<Double>>> folds = new LinkedHashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] fields = line.split(",", -1);
            if (fields.length != header.size()) {
                throw new IllegalArgumentException("Numero di colonne inatteso in " + csvFile + ": " + line);
            }
            Map<Metric, List<Double>> values = folds.computeIfAbsent(fields[schemeColumn],
                    scheme -> new EnumMap<>(Metric.class));
            metricColumns.forEach((metric, column) -> values.computeIfAbsent(metric, key -> new ArrayList<>())
                    .add(Double.parseDouble(fields[column])));
        }
        return folds;
    }

    private static int columnOf(List<String> header, String column, Path csvFile) {
        int index = header.indexOf(column);
        if (index < 0) {
            throw new IllegalArgumentException("Colonna " + column + " assente in " + csvFile);
        }
        return index;
    }
}
