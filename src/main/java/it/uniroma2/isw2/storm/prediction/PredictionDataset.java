package it.uniroma2.isw2.storm.prediction;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Dataset della Milestone 1 pronto per Weka. Le colonne identificative non diventano attributi: il Release ID serve
 * solo a scegliere le righe di training e di test. Le metriche sono trasformate con log10(x + 1), che porta 0 in 0,
 * e la classe Bugginess è l'ultimo attributo.
 */
public final class PredictionDataset {

    public static final String POSITIVE_CLASS = "Yes";

    private static final String RELEASE_ID_COLUMN = "Release ID";
    private static final List<String> IDENTIFIER_COLUMNS = List.of("Project Name", "Class Name", RELEASE_ID_COLUMN);
    private static final String CLASS_COLUMN = "Bugginess";
    // Yes per primo: Weka calcola Precision e Recall sul primo valore della classe.
    private static final List<String> CLASS_VALUES = List.of(POSITIVE_CLASS, "No");

    private final List<String> metricNames;
    private final List<LabeledRow> rows;

    private PredictionDataset(List<String> metricNames, List<LabeledRow> rows) {
        this.metricNames = metricNames;
        this.rows = rows;
    }

    public static PredictionDataset read(Path datasetCsv) throws IOException {
        List<String> lines = Files.readAllLines(datasetCsv, StandardCharsets.UTF_8);
        if (lines.size() < 2) {
            throw new IllegalArgumentException("Nessuna riga in " + datasetCsv);
        }
        List<String> header = Arrays.asList(lines.get(0).split(",", -1));
        requireLayout(header, datasetCsv);
        List<String> metricNames = List.copyOf(header.subList(IDENTIFIER_COLUMNS.size(), header.size() - 1));
        List<LabeledRow> rows = lines.stream().skip(1).map(line -> toRow(line, header.size(), datasetCsv)).toList();
        return new PredictionDataset(metricNames, rows);
    }

    public int metricCount() {
        return metricNames.size();
    }

    public int lastRelease() {
        return rows.stream().mapToInt(LabeledRow::releaseId).max().orElseThrow();
    }

    /** Righe delle release accettate dal filtro, con le sole metriche e la classe. */
    public Instances instances(String relationName, IntPredicate selectedRelease) {
        Instances instances = new Instances(relationName, new ArrayList<>(attributes()), rows.size());
        instances.setClassIndex(instances.numAttributes() - 1);
        rows.stream()
                .filter(row -> selectedRelease.test(row.releaseId()))
                .forEach(row -> instances.add(toInstance(row, instances)));
        return instances;
    }

    private List<Attribute> attributes() {
        List<Attribute> attributes = new ArrayList<>(metricNames.stream().map(Attribute::new).toList());
        attributes.add(new Attribute(CLASS_COLUMN, CLASS_VALUES));
        return attributes;
    }

    private static DenseInstance toInstance(LabeledRow row, Instances dataset) {
        double[] values = new double[dataset.numAttributes()];
        for (int metric = 0; metric < row.metrics().size(); metric++) {
            values[metric] = row.metrics().get(metric);
        }
        values[dataset.classIndex()] = dataset.classAttribute().indexOfValue(row.bugginess());
        return new DenseInstance(1.0, values);
    }

    private static void requireLayout(List<String> header, Path datasetCsv) {
        boolean identifiersFirst = header.size() > IDENTIFIER_COLUMNS.size() + 1
                && header.subList(0, IDENTIFIER_COLUMNS.size()).equals(IDENTIFIER_COLUMNS);
        if (!identifiersFirst || !header.get(header.size() - 1).equals(CLASS_COLUMN)) {
            throw new IllegalArgumentException("Intestazione inattesa in " + datasetCsv);
        }
    }

    private static LabeledRow toRow(String line, int columns, Path datasetCsv) {
        String[] fields = line.split(",", -1);
        if (fields.length != columns) {
            throw new IllegalArgumentException("Numero di colonne inatteso in " + datasetCsv + ": " + line);
        }
        String bugginess = fields[columns - 1];
        if (!CLASS_VALUES.contains(bugginess)) {
            throw new IllegalArgumentException("Valore di " + CLASS_COLUMN + " non valido in " + datasetCsv + ": " + line);
        }
        try {
            int releaseId = Integer.parseInt(fields[IDENTIFIER_COLUMNS.indexOf(RELEASE_ID_COLUMN)]);
            List<Double> metrics = Arrays.stream(fields, IDENTIFIER_COLUMNS.size(), columns - 1)
                    .map(PredictionDataset::logTransform)
                    .toList();
            return new LabeledRow(releaseId, metrics, bugginess);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valore non numerico in " + datasetCsv + ": " + line, e);
        }
    }

    private static double logTransform(String value) {
        double metric = Double.parseDouble(value);
        if (metric < 0) {
            throw new IllegalArgumentException("Metrica negativa, il logaritmo non è definito: " + value);
        }
        return Math.log10(metric + 1);
    }

    private record LabeledRow(int releaseId, List<Double> metrics, String bugginess) {
    }
}
